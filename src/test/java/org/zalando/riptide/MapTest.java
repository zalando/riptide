package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.model.Account;
import org.zalando.riptide.model.AccountBody;
import org.zalando.riptide.model.CheckedException;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public final class MapTest {

    private static final String REVISION = '"' + "1aa9520a-0cdd-11e5-aa27-8361dd72e660" + '"';

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final RestTemplate template;
    private final Rest unit;
    private final MockRestServiceServer server;

    public MapTest() {
        template = new RestTemplate();
        template.setMessageConverters(singletonList(new MappingJackson2HttpMessageConverter(
                new ObjectMapper().findAndRegisterModules())));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldCaptureResponse() {
        answerWithAccount();
        final Account account = dispatch(on(OK).capture(this::fromResponse));
        verify(account);
    }

    private Account fromResponse(final ClientHttpResponse response) throws IOException {
        final AccountBody account = new HttpMessageConverterExtractor<>(AccountBody.class,
                template.getMessageConverters()).extractData(response);
        return new Account(account.getId(), "fake", account.getName());
    }

    @Test
    public void shouldCaptureEntity() {
        answerWithAccount();
        final Account account = dispatch(
                on(OK).capture(AccountBody.class, this::fromEntity));
        verify(account);
    }

    private Account fromEntity(final AccountBody account) {
        return new Account(account.getId(), "fake", account.getName());
    }

    @Test
    public void shouldCaptureResponseEntity() {
        answerWithAccount();
        final Account account = dispatch(on(OK).capture(AccountBody.class, this::fromResponseEntity));
        verify(account, REVISION);
    }

    private Account fromResponseEntity(final ResponseEntity<AccountBody> entity) {
        final AccountBody account = entity.getBody();
        final String revision = entity.getHeaders().getETag();
        return new Account(account.getId(), revision, account.getName());
    }

    @Test(expected = CheckedException.class)
    public void shouldThrowCheckedExceptionOnResponse() {
        answerWithAccount();
        dispatch(on(OK).capture(this::validateResponse));
    }

    private Account validateResponse(final ClientHttpResponse response) throws CheckedException {
        throw new CheckedException();
    }

    @Test(expected = CheckedException.class)
    public void shouldThrowCheckedExceptionOnEntity() {
        answerWithAccount();
        dispatch(on(OK).capture(AccountBody.class, this::validateEntity));
    }

    private Account validateEntity(final AccountBody account) throws CheckedException {
        throw new CheckedException();
    }

    @Test(expected = CheckedException.class)
    public void shouldThrowCheckedExceptionOnResponseEntity() {
        answerWithAccount();
        dispatch(on(OK).capture(AccountBody.class, this::validateResponseEntity));
    }

    private Account validateResponseEntity(final AccountBody account) throws CheckedException {
        throw new CheckedException();
    }

    private void answerWithAccount() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setETag(REVISION);

        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON)
                        .headers(headers));
    }

    private Account dispatch(final Binding<HttpStatus> capturer) {
        return unit.execute(GET, url)
                .dispatch(status(),
                        capturer,
                        anyStatus().call(this::fail))
                .to(Account.class);
    }

    private void verify(final Account account) {
        verify(account, "fake");
    }

    private void verify(final Account account, final String revision) {
        assertThat(account.getId(), is("1234567890"));
        assertThat(account.getRevision(), is(revision));
        assertThat(account.getName(), is("Acme Corporation"));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
