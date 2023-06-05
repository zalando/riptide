package org.zalando.riptide.opentracing;

import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.mock.MockTracer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentracing.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.opentracing.MockWebServerUtil.verify;

final class ActivationTest {

    private final MockWebServer server = new MockWebServer();
    private final MockTracer tracer = new MockTracer();

    private final Http unit = Http.builder()
            .executor(new TracedExecutorService(newSingleThreadExecutor(), tracer))
            .requestFactory(new HttpComponentsClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .plugin(new OpenTracingPlugin(tracer)
                .withActivation(new NoOpActivation()))
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
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        unit.get("/users/{user}", "me")
                .call(pass())
                .join();

        assertThat(tracer.finishedSpans(), hasSize(1));
        verify(server, 1, "/users/me", GET.toString(), headers -> {
            assertNotNull(headers.get("traceid"));
            assertNotNull(headers.get("spanid"));
        });

    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

}
