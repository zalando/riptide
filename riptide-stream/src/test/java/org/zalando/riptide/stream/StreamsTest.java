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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.riptide.Rest;
import org.zalando.riptide.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Route.listOf;
import static org.zalando.riptide.stream.Streams.APPLICATION_JSON_SEQ;
import static org.zalando.riptide.stream.Streams.APPLICATION_X_JSON_STREAM;
import static org.zalando.riptide.stream.Streams.forEach;
import static org.zalando.riptide.stream.Streams.streamConverter;
import static org.zalando.riptide.stream.Streams.streamOf;

public class StreamsTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String baseUrl = "https://api.example.com";
    private final URI url = URI.create(baseUrl + "/accounts");

    private final Rest unit;
    private final MockRestServiceServer server;

    public StreamsTest() {
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final MockSetup setup = new MockSetup(baseUrl, singletonList(streamConverter(mapper)));
        this.server = setup.getServer();
        this.unit = setup.getRest();
    }

    @Test
    public void shouldCreateStreamConverter() {
        assertNotNull(streamConverter());
        assertNotNull(streamConverter(null));
        assertNotNull(streamConverter(null, null));
    }

    @Test
    public void shouldCallConsumerWithList() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<List<AccountBody>> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(listOf(AccountBody.class)), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(Arrays.asList(
                new AccountBody("1234567890", "Acme Corporation"),
                new AccountBody("1234567891", "Acme Company"),
                new AccountBody("1234567892", "Acme GmbH"),
                new AccountBody("1234567893", "Acme SE")));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldCallConsumerWithArray() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

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
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldCallConsumerWithJsonList() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldCallConsumerWithXJsonStream() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-stream.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldCallConsumerWithJsonSequence() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-sequence.json"))
                        .contentType(APPLICATION_JSON_SEQ));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCallConsumerWithoutStream() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-item.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(AccountBody.class, verifier),
                anyStatus().call(this::fail)).get();

        verify(verifier).accept(
                new AccountBody("1234567890", "Acme Corporation"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldNotCallConsumerForEmptyStream() throws Exception {
        final InputStream stream = new ByteArrayInputStream(new byte[0]);

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new InputStreamResource(stream))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).get();

        verifyZeroInteractions(verifier);
    }

    @Test
    public void shouldFailOnCallWithConsumerException() throws Exception {
        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(IOException.class));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-sequence.json"))
                        .contentType(APPLICATION_JSON_SEQ));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);
        doThrow(new IOException()).when(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));

        final Future<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verifyNoMoreInteractions(verifier);

        future.get();
    }

    @Test
    public void shouldFailOnCallWithInvalidStream() throws Exception {
        exception.expect(ExecutionException.class);
        exception.expectCause(instanceOf(UncheckedIOException.class));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-fail.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody> verifier = mock(ThrowingConsumer.class);

        final Future<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verifyNoMoreInteractions(verifier);

        future.get();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }
}
