package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.*;
import io.opentracing.*;
import io.opentracing.contrib.concurrent.*;
import io.opentracing.mock.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.PassRoute.*;

final class LifecyclePolicyTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new OpenTracingPlugin(tracer)
                .withLifecyclePolicy(LifecyclePolicy.composite(
                        new ExplicitSpanLifecyclePolicy(),
                        new ActiveSpanLifecyclePolicy()
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
