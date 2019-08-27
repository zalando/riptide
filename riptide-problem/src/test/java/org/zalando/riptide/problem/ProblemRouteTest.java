package org.zalando.riptide.problem;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.test.web.client.*;
import org.zalando.fauxpas.*;
import org.zalando.problem.*;
import org.zalando.riptide.*;

import java.net.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.problem.ProblemRoute.*;

@ExtendWith(MockitoExtension.class)
final class ProblemRouteTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    @Mock
    private ThrowingConsumer<Exceptional, RuntimeException> consumer;

    @Mock
    private Route fallback;

    ProblemRouteTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    void shouldPropagateProblem() {
        perform("application/problem+json");
    }

    @Test
    void shouldPropagateLegacyProblem() {
        perform("application/x.problem+json");
    }

    @Test
    void shouldPropagateLegacyProblemWithAlternativeSpelling() {
        perform("application/x-problem+json");
    }

    private void perform(final String mediaType) {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(MediaType.parseMediaType(mediaType)));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get(url)
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()),
                                anySeries().call(problemHandling()))::join);

        assertThat(exception.getCause(), is(instanceOf(ThrowableProblem.class)));
    }

    @Test
    void shouldDelegateProblemHandling() {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(MediaType.parseMediaType("application/problem+json")));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling(consumer)))
                .join();

        verify(consumer).tryAccept(any());
    }

    @Test
    void shouldUseFallback() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body("Error!"));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling(fallback)))
                .join();

        verify(fallback).execute(any(), any());
    }

    @Test
    void shouldNotDelegateProblemHandlingAndUseFallback() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body("Error!"));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling(consumer, fallback)))
                .join();

        verifyZeroInteractions(consumer);
        verify(fallback).execute(any(), any());
    }

    @Test
    void shouldDelegateProblemHandlingAndNotUseFallback() {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(MediaType.parseMediaType("application/problem+json")));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling(consumer, fallback)))
                .join();

        verify(consumer).tryAccept(any());
        verifyZeroInteractions(fallback);
    }

}
