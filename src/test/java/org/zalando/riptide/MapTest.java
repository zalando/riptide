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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.zalando.riptide.model.Account;
import org.zalando.riptide.model.AccountBody;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;

public final class MapTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private static final String REVISION = '"' + "1aa9520a-0cdd-11e5-aa27-8361dd72e660" + '"';

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;
    private final List<HttpMessageConverter<?>> converters;

    public MapTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
        this.converters = setup.getConverters();
    }

    @Test
    public void shouldCaptureResponse() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();
        final Account account = dispatch(on(OK).capture(this::fromResponse));
        verify(account);
    }

    private Account fromResponse(final ClientHttpResponse response) throws IOException {
        final AccountBody account = new HttpMessageConverterExtractor<>(AccountBody.class, converters)
                .extractData(response);
        return new Account(account.getId(), "fake", account.getName());
    }

    @Test
    public void shouldCaptureEntity() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();
        final Account account = dispatch(
                on(OK).capture(AccountBody.class, this::fromEntity));
        verify(account);
    }

    private Account fromEntity(final AccountBody account) {
        return new Account(account.getId(), "fake", account.getName());
    }

    @Test
    public void shouldCaptureResponseEntity() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();
        final Account account = dispatch(on(OK).capture(AccountBody.class, this::fromResponseEntity));
        verify(account, REVISION);
    }

    private Account fromResponseEntity(final ResponseEntity<AccountBody> entity) {
        final AccountBody account = entity.getBody();
        final String revision = entity.getHeaders().getETag();
        return new Account(account.getId(), revision, account.getName());
    }

    @Test
    public void shouldThrowCheckedExceptionOnResponse() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));

        dispatch(on(OK).capture(this::validateResponse));
    }

    private Account validateResponse(final ClientHttpResponse response) throws IOException {
        throw new IOException();
    }

    @Test
    public void shouldThrowCheckedExceptionOnEntity() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));

        dispatch(on(OK).capture(AccountBody.class, this::validateEntity));
    }

    private Account validateEntity(final AccountBody account) throws IOException {
        throw new IOException();
    }

    @Test
    public void shouldThrowCheckedExceptionOnResponseEntity() throws ExecutionException, InterruptedException, IOException {
        answerWithAccount();

        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));

        dispatch(on(OK).capture(AccountBody.class, this::validateResponseEntity));
    }

    private Account validateResponseEntity(final AccountBody account) throws IOException {
        throw new IOException();
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

    private Account dispatch(final Binding<HttpStatus> capturer) throws ExecutionException, InterruptedException, IOException {
        return unit.get(url)
                .dispatch(status(),
                        capturer,
                        anyStatus().call(this::fail))
                .get().to(Account.class);
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
