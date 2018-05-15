package org.zalando.riptide.problem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.problem.Exceptional;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.Http;
import org.zalando.riptide.Route;

import java.net.URI;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

@RunWith(MockitoJUnitRunner.class)
public final class ProblemRouteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    @Mock
    private ThrowingConsumer<Exceptional, RuntimeException> consumer;

    @Mock
    private Route fallback;

    public ProblemRouteTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getRest();
        this.server = setup.getServer();
    }

    @Test
    public void shouldPropagateProblem() {
        perform("application/problem+json");
    }

    @Test
    public void shouldPropagateLegacyProblem() {
        perform("application/x.problem+json");
    }

    @Test
    public void shouldPropagateLegacyProblemWithAlternativeSpelling() {
        perform("application/x-problem+json");
    }

    private void perform(final String mediaType) {
        server.expect(requestTo(url))
                .andRespond(withStatus(BAD_REQUEST)
                        .body(new ClassPathResource("problem.json"))
                        .contentType(MediaType.parseMediaType(mediaType)));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(ThrowableProblem.class));

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().call(problemHandling()))
                .join();
    }

    @Test
    public void shouldDelegateProblemHandling() {
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
    public void shouldUseFallback() throws Exception {
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
    public void shouldNotDelegateProblemHandlingAndUseFallback() throws Exception {
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
    public void shouldDelegateProblemHandlingAndNotUseFallback() throws Exception {
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
