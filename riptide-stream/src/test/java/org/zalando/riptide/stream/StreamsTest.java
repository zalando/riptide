package org.zalando.riptide.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.riptide.Http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Types.listOf;
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

    private final Http unit;
    private final MockRestServiceServer server;

    public StreamsTest() {
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final MockSetup setup = new MockSetup(baseUrl, singletonList(streamConverter(mapper)));
        this.server = setup.getServer();
        this.unit = setup.getRest();
    }

    @Test
    public void shouldCallConsumerWithList() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-list.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<List<AccountBody>, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(listOf(AccountBody.class)), forEach(verifier)),
                anyStatus().call(this::fail)).join();

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
        final ThrowingConsumer<AccountBody[], Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody[].class), forEach(verifier)),
                anyStatus().call(this::fail)).join();

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
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).join();

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
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).join();

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
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).join();

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier).accept(new AccountBody("1234567893", "Acme SE"));
        verifyNoMoreInteractions(verifier);
    }

    @Test
    public void shouldNotCallConsumerForEmptyStream() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(0);

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .headers(headers)
                        .body(new InputStreamResource(new ByteArrayInputStream(new byte[0])))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail)).join();

        verifyZeroInteractions(verifier);
    }

    @Test
    public void shouldFailOnCallWithConsumerException() throws Exception {
        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(UncheckedIOException.class));
        exception.expectCause(hasFeature(Throwable::getCause, instanceOf(IOException.class)));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-sequence.json"))
                        .contentType(APPLICATION_JSON_SEQ));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);
        doCallRealMethod().when(verifier).accept(any());
        doThrow(new IOException()).when(verifier).tryAccept(new AccountBody("1234567892", "Acme GmbH"));

        final CompletableFuture<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verify(verifier).accept(new AccountBody("1234567891", "Acme Company"));
        verify(verifier).accept(new AccountBody("1234567892", "Acme GmbH"));
        verify(verifier, times(3)).tryAccept(any());
        verifyNoMoreInteractions(verifier);

        future.join();
    }

    @Test
    public void shouldFailOnCallWithInvalidStream() throws Exception {
        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(UncheckedIOException.class));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account-fail.json"))
                        .contentType(APPLICATION_X_JSON_STREAM));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        final CompletableFuture<Void> future = unit.get("/accounts").dispatch(status(),
                on(OK).call(streamOf(AccountBody.class), forEach(verifier)),
                anyStatus().call(this::fail));

        verify(verifier).accept(new AccountBody("1234567890", "Acme Corporation"));
        verifyNoMoreInteractions(verifier);

        future.join();
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }
}
