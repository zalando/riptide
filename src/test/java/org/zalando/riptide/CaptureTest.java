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

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyStatusCode;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.statusCode;

public final class CaptureTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final RestTemplate template = new RestTemplate();
    private final Rest unit = Rest.create(template);

    private final MockRestServiceServer server = MockRestServiceServer.createServer(template);

    @Test
    public void shouldCapture() {
        server.expect(requestTo(url)).andRespond(
                withSuccess().body(new ClassPathResource("account.json")));
        
        final AccountRrepresentation account = unit.execute(GET, url)
                .dispatch(statusCode(),
                        on(OK, AccountRrepresentation.class).capture(),
                        anyStatusCode().call(this::fail))
                .retrieve(AccountRrepresentation.class).get();

        assertThat(account.id, is("1234567890"));
        assertThat(account.name, is("Acme Corporation"));
    }

    @Test
    public void shouldCaptureCall() {
        final String revision = "1aa9520a-0cdd-11e5-aa27-8361dd72e660";

        final HttpHeaders headers = new HttpHeaders();
        headers.setETag('"' + revision + '"');

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .headers(headers));

        final Account account = unit.execute(GET, url)
                .dispatch(statusCode(),
                        on(OK, AccountRrepresentation.class).map(this::extract).capture(),
                        anyStatusCode().call(this::fail))
                .retrieve(Account.class).get();

        assertThat(account.id, is("1234567890"));
        assertThat(account.revision, is(revision));
        assertThat(account.name, is("Acme Corporation"));
    }

    private Account extract(ResponseEntity<AccountRrepresentation> entity) {
        final AccountRrepresentation account = entity.getBody();
        final String revision = entity.getHeaders().getETag();
        return new Account(account.id, revision, account.name);
    }

    private void fail(ClientHttpResponse response) {
        try {
            throw new AssertionError(response.getRawStatusCode());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class AccountRrepresentation {
        private final String id;
        private final String name;

        private AccountRrepresentation(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class Account {
        private final String id;
        private final String revision;
        private final String name;

        private Account(String id, String revision, String name) {
            this.id = id;
            this.revision = revision;
            this.name = name;
        }
    }

}
