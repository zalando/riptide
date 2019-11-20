package org.zalando.riptide.opentracing;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.zalando.riptide.PassRoute.pass;

final class InjectionTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new OpenTracingPlugin(tracer)
                .withInjection(new NoOpInjection()))
            .build();

    @Test
    void shouldNotInject() {
        driver.addExpectation(onRequestTo("/users/me")
                        .withoutHeader("traceid")
                        .withoutHeader("spanid"),
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
