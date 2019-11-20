package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.opentracing.Scope;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.zalando.riptide.PassRoute.pass;

final class LifecycleTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new OpenTracingPlugin(tracer)
                .withLifecycle(Lifecycle.composite(
                        new ExplicitSpanLifecycle(),
                        new ActiveSpanLifecycle()
                )))
            .build();

    @Test
    void shouldFallbackToNoOp() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withoutHeader("traceid")
                        .withoutHeader("spanid"),
                giveEmptyResponse().withStatus(200));

            unit.get("/users/{user}", "me")
                    .call(pass())
                    .join();

        assertThat(tracer.finishedSpans(), is(empty()));
    }

    @Test
    void shouldUseExplicitSpan() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withHeader("traceid", notNullValue(String.class))
                        .withHeader("spanid", notNullValue(String.class)),
                giveEmptyResponse().withStatus(200));

        final MockSpan span = tracer.buildSpan("test").start();

        unit.get("/users/{user}", "me")
                .attribute(OpenTracingPlugin.SPAN, span)
                .call(pass())
                .join();

        span.finish();

        assertThat(tracer.finishedSpans(), contains(span));
    }

    @Test
    void shouldUseActiveSpan() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withHeader("traceid", notNullValue(String.class))
                        .withHeader("spanid", notNullValue(String.class)),
                giveEmptyResponse().withStatus(200));

        final MockSpan span = tracer.buildSpan("test").start();

        try (final Scope ignored = tracer.activateSpan(span)) {
            unit.get("/users/{user}", "me")
                    .call(pass())
                    .join();
        } finally {
            span.finish();
        }

        assertThat(tracer.finishedSpans(), contains(span));
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
