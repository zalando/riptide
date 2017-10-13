package org.zalando.riptide;

import com.google.common.io.ByteStreams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.problem.ThrowableProblem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.headers;
import static org.zalando.riptide.Route.location;
import static org.zalando.riptide.Route.propagate;
import static org.zalando.riptide.Route.to;

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

    @Deprecated
    @Test
    public void shouldMapHeaders() {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new ClassPathResource("account.json"))
                        .contentType(APPLICATION_JSON));

        final AtomicReference<HttpHeaders> capture = new AtomicReference<>();

        unit.head(url)
                .dispatch(status(),
                        on(OK).call(to(headers()).andThen(capture::set)),
                        anyStatus().call(this::fail))
                .join();

        final HttpHeaders headers = capture.get();
        assertThat(headers.toSingleValueMap(), hasEntry("Content-Type", APPLICATION_JSON_VALUE));
    }

    @Deprecated
    @Test
    public void shouldMapLocation() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("http://example.org"));

        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .headers(headers));

        final AtomicReference<URI> capture = new AtomicReference<>();

        unit.head(url)
                .dispatch(status(),
                        on(OK).call(to(location()).andThen(capture::set)),
                        anyStatus().call(this::fail))
                .join();

        final URI uri = capture.get();
        assertThat(uri, hasToString("http://example.org"));
    }

    @Test
    public void shouldThrowNoRouteExceptionWithContent() {
        server.expect(requestTo(url)).andRespond(
                withStatus(CONFLICT)
                        .body("verbose body content")
                        .contentType(MediaType.TEXT_PLAIN));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));
        exception.expectCause(
                hasFeature(Throwable::getMessage, containsString("Content-Type=[" + MediaType.TEXT_PLAIN + "]")));
        exception.expectCause(hasFeature(Throwable::getMessage, containsString("verbose body content")));

        unit.get(url)
                .dispatch(status(),
                        anyStatus().call(noRoute()))
                .join();
    }

    @Deprecated
    @Test
    public void shouldDoNothing() {
        Route.pass().tryAccept(null);
    }

    @Test
    public void shouldThrowNoRouteExceptionWithoutContent() {
        server.expect(requestTo(url)).andRespond(withStatus(NO_CONTENT));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));

        unit.get(url)
                .dispatch(status(),
                        anyStatus().call(noRoute()))
                .join();
    }

    @Deprecated
    @Test(expected = NoRouteException.class)
    public void shouldThrowNoRouteException() throws IOException {
        Route.noRoute().tryAccept(new MockClientHttpResponse(new byte[0], OK));
    }

    @Deprecated
    @Test
    public void shouldWrapAndPropagateException() throws IOException {
        exception.expect(IOException.class);
        exception.expectCause(instanceOf(URISyntaxException.class));

        propagate().tryAccept(new URISyntaxException("foo", "bar"));
    }

    @Deprecated
    @Test
    public void shouldPropagateRuntimeExceptionAsIs() {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ThrowableProblem.class));

        unit.get(url)
                .dispatch(status(),
                        on(UNPROCESSABLE_ENTITY).call(ThrowableProblem.class, propagate()),
                        anyStatus().call(this::fail))
                .join();
    }

    @Deprecated
    @Test
    public void shouldPropagateIOExceptionAsIs() {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body("{}")
                        .contentType(APPLICATION_JSON));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(IOException.class));

        unit.get(url)
                .dispatch(status(),
                        on(UNPROCESSABLE_ENTITY).call(IOException.class, propagate()),
                        anyStatus().call(this::fail))
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
