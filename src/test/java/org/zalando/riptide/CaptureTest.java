package org.zalando.riptide;

/*
 * ⁣​
 * riptide
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
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.PassThroughResponseErrorHandler;
import org.zalando.riptide.model.Account;
import org.zalando.riptide.model.AccountBody;

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

public final class CaptureTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public CaptureTest() {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldCaptureResponse() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));
        
        final ClientHttpResponse response = unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).capture(),
                        anyStatus().call(this::fail))
                .retrieveResponse().get();
        
        assertThat(response.getStatusCode(), is(OK));
        assertThat(response.getHeaders().getContentType(), is(APPLICATION_JSON));
    }
    
    @Test
    public void shouldCaptureEntity() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final AccountBody account = unit.execute(GET, url)
                .dispatch(status(),
                        on(OK, AccountBody.class).capture(),
                        anyStatus().call(this::fail))
                .retrieve(AccountBody.class).get();

        assertThat(account.getId(), is("1234567890"));
        assertThat(account.getName(), is("Acme Corporation"));
    }

    @Test
    public void shouldCaptureMappedEntity() {
        final String revision = '"' + "1aa9520a-0cdd-11e5-aa27-8361dd72e660" + '"';

        final HttpHeaders headers = new HttpHeaders();
        headers.setETag(revision);

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON)
                        .headers(headers));

        final Account account = unit.execute(GET, url)
                .dispatch(status(),
                        on(OK, AccountBody.class).map(this::extract).capture(),
                        anyStatus().call(this::fail))
                .retrieve(Account.class).get();

        assertThat(account.getId(), is("1234567890"));
        assertThat(account.getRevision(), is(revision));
        assertThat(account.getName(), is("Acme Corporation"));
    }

    private Account extract(ResponseEntity<AccountBody> entity) {
        final AccountBody account = entity.getBody();
        final String revision = entity.getHeaders().getETag();
        return new Account(account.getId(), revision, account.getName());
    }

    private void fail(ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
