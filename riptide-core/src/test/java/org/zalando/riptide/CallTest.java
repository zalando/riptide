package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.fauxpas.ThrowingRunnable;
import org.zalando.riptide.model.AccountBody;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.Types.responseEntityOf;

public final class CallTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    public CallTest() {
        final MockSetup setup = new MockSetup("https://api.example.com/");
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    public void shouldCallEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<AccountBody, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(AccountBody.class, verifier),
                        anyStatus().call(this::fail));

        verify(verifier).tryAccept(any(AccountBody.class));
    }

    @Test
    public void shouldCallResponseEntity() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingConsumer<ResponseEntity<AccountBody>, Exception> verifier = mock(ThrowingConsumer.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(responseEntityOf(AccountBody.class), verifier),
                        anyStatus().call(this::fail));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<ResponseEntity<AccountBody>> captor = ArgumentCaptor.forClass(ResponseEntity.class);
        verify(verifier).tryAccept(captor.capture());
        final ResponseEntity<AccountBody> entity = captor.getValue();

        assertThat(entity.getStatusCode(), is(OK));
        assertThat(entity.getHeaders(), is(not(anEmptyMap())));
        assertThat(entity.getBody().getName(), is("Acme Corporation"));
    }

    @Test
    public void shouldCallWithoutParameters() throws Exception {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        @SuppressWarnings("unchecked")
        final ThrowingRunnable<Exception> verifier = mock(ThrowingRunnable.class);

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(verifier),
                        anyStatus().call(this::fail));

        verify(verifier).tryRun();
    }

    @Test
    public void shouldThrowCheckedExceptionOnEntity() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(IOException.class));

        unit.get("accounts/123")
                .dispatch(status(),
                        on(OK).call(AccountBody.class, this::validateEntity),
                        anyStatus().call(this::fail))
                .join();
    }

    private void validateEntity(final AccountBody account) throws IOException {
        throw new IOException("Account " + account + " is invalid");
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
