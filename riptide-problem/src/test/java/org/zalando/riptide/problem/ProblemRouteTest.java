package org.zalando.riptide.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.ErrorResponseException;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.problem.Exceptional;
import org.zalando.problem.ThrowableProblem;
import org.zalando.riptide.Http;
import org.zalando.riptide.Route;

import java.net.URI;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.problem.ProblemRoute.problemHandling;

@ExtendWith(MockitoExtension.class)
final class ProblemRouteTest {

    private final URI url = URI.create("https://api.example.com/accounts/123");

    private final Http unit;
    private final MockRestServiceServer server;

    @Mock
    private ThrowingConsumer<ProblemDetail, RuntimeException> consumer;

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

        assertThat(exception.getCause(), is(instanceOf(ProblemResponseException.class)));
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

        verifyNoInteractions(consumer);
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
        verifyNoInteractions(fallback);
    }
}
