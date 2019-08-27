package org.zalando.riptide;

import com.google.common.io.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.NoRoute.*;
import static org.zalando.riptide.PassRoute.*;

final class RouteTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    RouteTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    void shouldPass() {
        server.expect(requestTo(url)).andRespond(
                withSuccess());

        unit.get(url)
                .dispatch(status(),
                        on(OK).call(pass()),
                        anyStatus().call(this::fail));
    }

    @Test
    void shouldThrowNoRouteExceptionWithContent() {
        server.expect(requestTo(url)).andRespond(
                withStatus(CONFLICT)
                        .body("verbose body content")
                        .contentType(TEXT_PLAIN));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get(url)
                        .dispatch(status(),
                                anyStatus().call(noRoute()))
                        .join());
        assertThat(exception.getCause(), is(instanceOf(UnexpectedResponseException.class)));
        assertThat(exception.getMessage(), containsString(TEXT_PLAIN_VALUE));
        assertThat(exception.getMessage(), containsString("Content-Type"));
        assertThat(exception.getMessage(), containsString("verbose body content"));
    }

    @Test
    void shouldThrowNoRouteExceptionWithoutContent() {
        server.expect(requestTo(url)).andRespond(withStatus(NO_CONTENT));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get(url)
                        .dispatch(status(),
                                anyStatus().call(noRoute()))
                        .join());

        assertThat(exception.getCause(), is(instanceOf(UnexpectedResponseException.class)));
    }

    @Test
    void shouldAllowToCaptureBody() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{}")
                        .contentType(APPLICATION_JSON));

        final AtomicReference<InputStream> body = new AtomicReference<>();

        unit.get(url).call(((response, reader) ->
                body.set(response.getBody())))
                .join();

        // read response outside of consumer/callback
        // to make sure the stream is still available
        final byte[] bytes = ByteStreams.toByteArray(body.get());
        assertThat(bytes.length, is(greaterThan(0)));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
