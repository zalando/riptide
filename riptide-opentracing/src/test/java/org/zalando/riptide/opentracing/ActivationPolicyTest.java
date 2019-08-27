package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.*;
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

final class ActivationPolicyTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new OpenTracingPlugin(tracer)
                .withActivationPolicy(new NoOpActivationPolicy()))
            .plugin(new Plugin() {
                @Override
                public RequestExecution aroundNetwork(final RequestExecution execution) {
                    return arguments -> {
                        assertThat(tracer.activeSpan(), is(nullValue()));
                        return execution.execute(arguments);
                    };
                }
            })
            .build();

    @Test
    void shouldNotActivate() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withHeader("traceid", notNullValue(String.class))
                        .withHeader("spanid", notNullValue(String.class)),
                giveEmptyResponse().withStatus(200));

            unit.get("/users/{user}", "me")
                    .call(pass())
                    .join();

        assertThat(tracer.finishedSpans(), hasSize(1));
    }

    @AfterEach
    void tearDown() {
        driver.verify();
        driver.shutdown();
    }

}
