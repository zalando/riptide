package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.zalando.riptide.PassRoute.pass;

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
