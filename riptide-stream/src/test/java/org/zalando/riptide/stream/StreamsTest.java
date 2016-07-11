package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide: Stream
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.MockSetup;
import org.zalando.riptide.Rest;
import org.zalando.riptide.ThrowingConsumer;
import org.zalando.riptide.model.AccountBody;
import org.zalando.riptide.stream.Streams;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.stream.Streams.forEach;
import static org.zalando.riptide.stream.Streams.streamOf;
import static org.zalando.riptide.Route.listOf;

public class StreamsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String baseUrl = "https://api.example.com";
    private final URI url = URI.create(baseUrl + "/accounts");

    private final Rest unit;
    private final MockRestServiceServer server;

    public StreamsTest() {
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MockSetup setup = new MockSetup(baseUrl, Arrays.asList(Streams.streamConverter(mapper)));
        this.server = setup.getServer();
        this.unit = setup.getRest();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCallConsumerWithList() throws Exception {
        final InputStream stream = new ClassPathResource("account-list.json").getInputStream();
    
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));
    
        final ThrowingConsumer<List<AccountBody>> verifier = mock(ThrowingConsumer.class);
    
        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(listOf(AccountBody.class)), forEach(verifier)),
                anyStatus().call(this::fail)).get();
    
        verify(verifier).accept(Arrays.asList(
                new AccountBody("1234567890", "Acme Corporation"),
                new AccountBody("1234567891", "Acme Company"),
                new AccountBody("1234567892", "Acme GmbH"),
                new AccountBody("1234567893", "Acme SE")));
        verify(verifier, times(1)).accept(any(List.class));
    }

    @Test
    public void shouldCallConsumerWithArray() throws Exception {
        final InputStream stream = new ClassPathResource("account-list.json").getInputStream();
    
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));
    
        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody[]> verifier = mock(ThrowingConsumer.class);
    
        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody[].class), forEach(verifier)),
                anyStatus().call(this::fail)).get();
    
        verify(verifier).accept(new AccountBody[] {
                new AccountBody("1234567890", "Acme Corporation"),
                new AccountBody("1234567891", "Acme Company"),
                new AccountBody("1234567892", "Acme GmbH"),
                new AccountBody("1234567893", "Acme SE") });
        verify(verifier, times(1)).accept(any(AccountBody[].class));
    }

    @Test
    public void shouldCallConsumerWithJsonList() throws Exception {
        final InputStream stream = new ClassPathResource("account-list.json").getInputStream();
    
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));
    
        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);
    
        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();
    
        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    public void shouldCallConsumerWithXJsonStream() throws Exception {
        final InputStream stream = new ClassPathResource("account-stream.json").getInputStream();

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    public void shouldCallConsumerWithJsonSequence() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verify(verifier, times(4)).accept(any(AccountBody.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCallConsumerWithoutStream() throws Exception {
        final InputStream stream = new ClassPathResource("account-item.json").getInputStream();
    
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));
    
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);
    
        unit.get("/accounts").dispatch(status(),
                on(OK).call(AccountBody.class, verifier),
                anyStatus().call(this::fail)).get();
    
        verify(verifier).accept(
                new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier, times(1)).accept(any(AccountBody.class));
    }

    @Test
    public void shouldNotCallConsumerForEmptyStream() throws Exception {
        final InputStream stream = new ByteArrayInputStream(new byte[0]);

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier, never()).accept(any(AccountBody.class));
        ;
    }

    @Test
    public void shouldFailOnCallWithConsumerException() throws Exception {
        exception.expect(java.util.concurrent.ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));
    
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();
    
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));
    
        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);
        Mockito.doThrow(new IOException()).when(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
    
        Future<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));
    
        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier, times(3)).accept(any(AccountBody.class));
    
        future.get();
    }

    @Test
    public void shouldFailOnCallWithInvalidStream() throws Exception {
        exception.expect(java.util.concurrent.ExecutionException.class);
        exception.expectCause(instanceOf(UncheckedIOException.class));

        final InputStream stream = new ClassPathResource("account-fail.json").getInputStream();

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        Future<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier, times(1)).accept(any(AccountBody.class));

        future.get();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }
}
