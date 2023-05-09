package org.zalando.riptide;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.zalando.riptide.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.MockWebServerUtil.getRecorderRequest;
import static org.zalando.riptide.PassRoute.pass;

final class EmptyEntityTest {

    private final MockWebServer server = new MockWebServer();

    private final ExecutorService executor = newSingleThreadExecutor();

    @AfterEach
    void shutDownExecutor() {
        executor.shutdown();
    }

    @SneakyThrows
    @AfterEach
    void shutdownDriver() {
        server.shutdown();
    }

    @Test
    void shouldPassEmptyEntity() {
        server.enqueue(emptyMockResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(getBaseUrl(server))
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertTrue(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.get("/")
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecorderRequest(server);
        verifyRequest(recordedRequest, "/", GET.toString(), "Passed", "true");
    }

    private void verifyRequest(RecordedRequest recordedRequest,
                               String expectedPath,
                               String expectedMethod,
                               String expectedKey,
                               String expectedValue) {
        assertNotNull(recordedRequest);
        assertEquals(expectedPath, recordedRequest.getPath());
        assertEquals(expectedMethod, recordedRequest.getMethod());
        assertEquals(expectedValue, recordedRequest.getHeaders().get(expectedKey));

    }

    @Test
    void shouldPassNonEmptyEntity() {
        server.enqueue(emptyMockResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(getBaseUrl(server))
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertFalse(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.post("/")
                .body(emptyMap())
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecorderRequest(server);
        verifyRequest(recordedRequest, "/", POST.toString(), "Passed", "true");

    }

    @Test
    void shouldPassExplicitNonEmptyEntity() {
        server.enqueue(emptyMockResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(getBaseUrl(server))
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertFalse(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.post("/")
                .body(message -> {})
                .call(pass())
                .join();

        RecordedRequest recordedRequest = getRecorderRequest(server);
        verifyRequest(recordedRequest, "/", POST.toString(), "Passed", "true");
    }


}
