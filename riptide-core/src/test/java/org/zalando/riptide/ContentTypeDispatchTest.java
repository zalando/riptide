package org.zalando.riptide;

import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.client.*;
import org.springframework.test.web.client.*;
import org.zalando.riptide.model.Error;
import org.zalando.riptide.model.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.model.MediaTypes.*;

final class ContentTypeDispatchTest {

    private final URI url = URI.create("https://api.example.com");

    private final Http unit;
    private final MockRestServiceServer server;

    ContentTypeDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    private <T> T perform(final Class<T> type) {
        final AtomicReference<Object> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(SUCCESS).call(Success.class, capture::set),
                        on(PROBLEM).call(Problem.class, capture::set),
                        on(ERROR).call(Error.class, capture::set),
                        anyContentType().call(this::fail))
                .join();

        return type.cast(capture.get());
    }

    @Test
    void shouldDispatchSuccess() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final Success success = perform(Success.class);

        assertThat(success.isHappy(), is(true));
    }

    @Test
    void shouldDispatchProblem() {
        server.expect(requestTo(url))
                .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(PROBLEM));

        final Problem problem = perform(Problem.class);

        assertThat(problem.getType(), is(URI.create("http://httpstatus.es/422")));
        assertThat(problem.getTitle(), is("Unprocessable Entity"));
        assertThat(problem.getStatus(), is(422));
        assertThat(problem.getDetail(), is("A problem occurred."));
    }

    @Test
    void shouldDispatchError() {
        server.expect(requestTo(url))
                .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("error.json"))
                        .contentType(ERROR));

        final Error error = perform(Error.class);

        assertThat(error.getMessage(), is("A problem occurred."));
        assertThat(error.getPath(), is(url));
    }

    @Test
    void shouldDispatchToMostSpecificContentType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS));

        final AtomicReference<Success> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(parseMediaType("application/*+json")).call(this::fail),
                        on(parseMediaType("application/success+json;version=2")).call(Success.class, capture::set),
                        anyContentType().call(this::fail))
                .join();

        final Success success = capture.get();
        assertThat(success.isHappy(), is(true));
    }

    @Test
    void shouldNotFailIfNoContentTypeSpecified() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(null));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(contentType(),
                                on(SUCCESS).call(pass())),
                        anySeries().call(pass()))
                .join();
    }

    @Test
    void shouldDispatchToFullMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(SUCCESS_V2));

        final AtomicReference<Success> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(contentType(),
                        on(SUCCESS_V1).call(this::fail),
                        on(SUCCESS_V2).call(Success.class, capture::set),
                        anyContentType().call(this::fail))
                .join();

        final Success success = capture.get();
        assertThat(success.isHappy(), is(true));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getStatusText());
    }

}
