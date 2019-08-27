package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;
import org.zalando.fauxpas.*;
import org.zalando.riptide.model.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.Types.*;

final class CallTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    CallTest() {
        final MockSetup setup = new MockSetup("https://api.example.com/");
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    void shouldCallEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked") final ThrowingConsumer<AccountBody, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(AccountBody.class, verifier),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).tryAccept(any(AccountBody.class));
    }

    @Test
    void shouldCallResponseEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked") final ThrowingConsumer<ResponseEntity<AccountBody>, Exception> verifier = mock(
                ThrowingConsumer.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(responseEntityOf(AccountBody.class), verifier),
                        anyStatus().call(this::fail))
                .join();

        @SuppressWarnings("unchecked") final ArgumentCaptor<ResponseEntity<AccountBody>> captor = ArgumentCaptor.forClass(
                ResponseEntity.class);
        verify(verifier).tryAccept(captor.capture());
        final ResponseEntity<AccountBody> entity = captor.getValue();

        assertThat(entity.getStatusCode(), is(OK));
        assertThat(entity.getHeaders(), is(not(anEmptyMap())));
        assertThat(entity.getBody().getName(), is("Acme Corporation"));
    }

    @Test
    void shouldCallWithoutParameters() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked") final ThrowingRunnable<Exception> verifier = mock(ThrowingRunnable.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(verifier),
                        anyStatus().call(this::fail))
                .join();

        verify(verifier).tryRun();
    }

    @Test
    void shouldThrowCheckedExceptionOnEntity() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get("accounts/123")
                        .dispatch(status(),
                                on(OK).call(AccountBody.class, this::validateEntity),
                                anyStatus().call(this::fail))
                        .join());

        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }

    private void validateEntity(final AccountBody account) throws IOException {
        throw new IOException("Account " + account + " is invalid");
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
