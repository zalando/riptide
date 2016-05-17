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
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.model.AccountBody;
import org.zalando.riptide.model.CheckedException;

import java.io.IOException;
import java.net.URI;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Conditions.anyStatus;
import static org.zalando.riptide.Conditions.on;
import static org.zalando.riptide.Selectors.status;

public final class CallTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Rest unit;
    private final MockRestServiceServer server;

    public CallTest() {
        final RestTemplate template = new RestTemplate();
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        template.setMessageConverters(singletonList(converter));
        template.setErrorHandler(new PassThroughResponseErrorHandler());
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.create(template);
    }

    @Test
    public void shouldCallEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ResponseEntityConsumer<AccountBody, Exception> verifier =
                mock(ResponseEntityConsumer.class);

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(AccountBody.class, verifier),
                        anyStatus().call(this::fail));

        verify(verifier).accept(anyResponseEntityOf(AccountBody.class));
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<T> anyResponseEntityOf(@SuppressWarnings("UnusedParameters") final Class<T> type) {
        return any(ResponseEntity.class);
    }

    @Test
    public void shouldCallResponseEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final EntityConsumer<AccountBody, Exception> verifier =
                mock(EntityConsumer.class);

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(AccountBody.class, verifier),
                        anyStatus().call(this::fail));

        verify(verifier).accept(any(AccountBody.class));
    }

    @Test
    public void shouldCallWithoutParameters() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingRunnable<Exception> verifier =
                mock(ThrowingRunnable.class);

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(verifier),
                        anyStatus().call(this::fail));

        verify(verifier).run();
    }

    @Test(expected = CheckedException.class)
    public void shouldThrowCheckedExceptionOnEntity() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(AccountBody.class, this::validateEntity),
                        anyStatus().call(this::fail));
    }

    private void validateEntity(final AccountBody account) throws CheckedException {
        throw new CheckedException();
    }

    @Test(expected = CheckedException.class)
    public void shouldThrowCheckedExceptionOnResponseEntity() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        unit.execute(GET, url)
                .dispatch(status(),
                        on(OK).call(AccountBody.class, this::validateResponse),
                        anyStatus().call(this::fail));
    }

    private void validateResponse(final ResponseEntity<AccountBody> account) throws CheckedException {
        throw new CheckedException();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
