package org.zalando.riptide.stream;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;
import org.zalando.fauxpas.*;
import org.zalando.riptide.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.Types.*;
import static org.zalando.riptide.stream.Streams.*;

final class StreamsTest {

    private final String baseUrl = "https://api.example.com";
    private final URI url = URI.create(baseUrl + "/accounts");

    private final Http unit;
    private final MockRestServiceServer server;

    StreamsTest() {
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final MockSetup setup = new MockSetup(baseUrl, singletonList(streamConverter(mapper)));
        this.server = setup.getServer();
        this.unit = setup.getRest();
    }

    @Test
    void shouldCallConsumerWithList() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<List<AccountBody>, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(listOf(AccountBody.class)), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).accept(Arrays.asList(
                new AccountBody("1234567890", "Acme Corporation"),
                new AccountBody("1234567891", "Acme Company"),
                new AccountBody("1234567892", "Acme GmbH"),
                new AccountBody("1234567893", "Acme SE")));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldCallConsumerWithArray() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody[], Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(AccountBody[].class), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).accept(new AccountBody[]{
                new AccountBody("1234567890", "Acme Corporation"),
                new AccountBody("1234567891", "Acme Company"),
                new AccountBody("1234567892", "Acme GmbH"),
                new AccountBody("1234567893", "Acme SE")});
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldCallConsumerWithJsonList() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldCallConsumerWithXJsonStream() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-stream.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldCallConsumerWithJsonSequence() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-sequence.json"))
                        .contentType(APPLICATION_JSON_SEQ));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldNotCallConsumerForEmptyStream() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(0);

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .headers(headers)
                        .body(new InputStreamResource(new ByteArrayInputStream(new byte[0])))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("/accounts")
                .dispatch(status(),
                        on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                        anyStatus().call(this::fail))
                .join();

        verifyZeroInteractions(verifier);
    }

    @Test
    void shouldFailOnCallWithConsumerException() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-sequence.json"))
                        .contentType(APPLICATION_JSON_SEQ));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);
        doCallRealMethod().when(verifier).accept(any());
        doThrow(new IOException()).when(verifier).tryAccept(new AccountBody("1234567892", "Acme GmbH"));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/accounts")
                        .dispatch(status(),
                                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                                anyStatus().call(this::fail))::join);

        assertThat(exception.getCause(), is(instanceOf(UncheckedIOException.class)));
        assertThat(exception.getCause().getCause(), is(instanceOf(IOException.class)));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier, times(3)).tryAccept(any());
        verifyNoMoreInteractions(verifier);
    }

    @Test
    void shouldFailOnCallWithInvalidStream() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-fail.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/accounts")
                        .dispatch(status(),
                                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                                anyStatus().call(this::fail))::join);

        assertThat(exception.getCause(), is(instanceOf(UncheckedIOException.class)));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verifyNoMoreInteractions(verifier);
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }
}
