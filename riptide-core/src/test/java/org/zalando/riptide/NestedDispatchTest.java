package org.zalando.riptide;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.problem.Exceptional;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.model.Message;
import org.zalando.riptide.model.Problem;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.RoutingTree.dispatch;
import static org.zalando.riptide.Types.listOf;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;

final class NestedDispatchTest {

    private final URI url = URI.create("http://localhost");

    private final Http unit;
    private final MockRestServiceServer server;

    NestedDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    private <T> T perform(final Class<T> type) {
        final AtomicReference<Object> capture = new AtomicReference<>();

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL)
                                .dispatch(status(),
                                        on(CREATED).dispatch(contentType(),
                                                on(parseMediaType("application/messages+json")).call(listOf(Message.class), capture::set),
                                                anyContentType().call(Success.class, capture::set)),
                                        on(ACCEPTED).call(Success.class, capture::set),
                                        anyStatus().call(this::fail)),
                        on(CLIENT_ERROR)
                                .dispatch(status(),
                                        on(UNAUTHORIZED).call(capture::set),
                                        anyStatus().call(problemHandling())),
                        on(SERVER_ERROR)
                                .dispatch(statusCode(),
                                        on(500).call(capture::set),
                                        on(503).call(capture::set),
                                        anyStatusCode().call(this::fail)),
                        anySeries().call(this::fail))
                .join();

        return type.cast(capture.get());
    }

    private Route problemHandling() {
        return dispatch(contentType(),
                on(PROBLEM).call(ThrowableProblem.class, Exceptional::propagate),
                on(ERROR).call(ThrowableProblem.class, Exceptional::propagate),
                anyContentType().call(this::fail));
    }

    @SuppressWarnings("serial")
    private static final class Failure extends RuntimeException {
        private final HttpStatus status;

        private Failure(final HttpStatus status) {
            this.status = status;
        }

        HttpStatus getStatus() {
            return status;
        }
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new Failure(HttpStatus.resolve(response.getStatusCode().value()));
    }

    @Test
    void shouldDispatchLevelOne() {
        server.expect(requestTo(url)).andRespond(withStatus(MOVED_PERMANENTLY));

        final CompletionException exception = assertThrows(CompletionException.class, () -> perform(Void.class));

        final Failure cause = (Failure) exception.getCause();
        assertThat(cause, hasFeature("status", Failure::getStatus, equalTo(MOVED_PERMANENTLY)));
    }

    @Test
    void shouldDispatchLevelTwo() {
        server.expect(requestTo(url)).andRespond(
                withStatus(CREATED)
                        .body(new ClassPathResource("messages.json"))
                        .contentType(parseMediaType("application/messages+json")));

        @SuppressWarnings("unchecked")
        final List<Message> messages = perform(List.class);

        assertThat(messages.get(0).getMessage(), is("Foo"));
        assertThat(messages.get(1).getMessage(), is("Bar"));
    }

    @Test
    void shouldDispatchLevelThree() {
        server.expect(requestTo(url)).andRespond(
                withStatus(UNPROCESSABLE_ENTITY)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(ERROR));

        try {
            perform(Problem.class);
            Assertions.fail("Expected exception");
        } catch (final CompletionException e) {
            assertThat(e.getCause(), is(instanceOf(ThrowableProblem.class)));
            final ThrowableProblem problem = (ThrowableProblem) e.getCause();
            assertThat(problem.getType(), is(URI.create("http://httpstatus.es/422")));
            assertThat(problem.getTitle(), is("Unprocessable Entity"));
            assertThat(problem.getStatus(), is(Status.UNPROCESSABLE_ENTITY));
            assertThat(problem.getDetail(), is("A problem occurred."));
        }
    }

}
