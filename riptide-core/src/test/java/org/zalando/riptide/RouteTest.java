package org.zalando.riptide;

import com.google.common.io.ByteStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;

public final class RouteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    public RouteTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    public void shouldPass() {
        server.expect(requestTo(url)).andRespond(
                withSuccess());

        unit.get(url)
                .dispatch(status(),
                        on(OK).call(pass()),
                        anyStatus().call(this::fail));
    }

    @Test
    public void shouldThrowNoRouteExceptionWithContent() {
        server.expect(requestTo(url)).andRespond(
                withStatus(CONFLICT)
                        .body("verbose body content")
                        .contentType(MediaType.TEXT_PLAIN));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(UnexpectedResponseException.class));
        exception.expectCause(
                hasFeature(Throwable::getMessage, containsString("Content-Type=[" + MediaType.TEXT_PLAIN + "]")));
        exception.expectCause(hasFeature(Throwable::getMessage, containsString("verbose body content")));

        unit.get(url)
                .dispatch(status(),
                        anyStatus().call(noRoute()))
                .join();
    }

    @Test
    public void shouldThrowNoRouteExceptionWithoutContent() {
        server.expect(requestTo(url)).andRespond(withStatus(NO_CONTENT));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(UnexpectedResponseException.class));

        unit.get(url)
                .dispatch(status(),
                        anyStatus().call(noRoute()))
                .join();
    }

    @Test
    public void shouldAllowToCaptureBody() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{}")
                        .contentType(APPLICATION_JSON));

        final AtomicReference<InputStream> body = new AtomicReference<>();

        unit.get(url).call(((response, reader) ->
                body.set(response.getBody())));

        // read response outside of consumer/callback
        // to make sure the stream is still available
        final byte[] bytes = ByteStreams.toByteArray(body.get());
        assertThat(bytes.length, is(greaterThan(0)));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
