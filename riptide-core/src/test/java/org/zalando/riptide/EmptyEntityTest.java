package org.zalando.riptide;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.PassRoute.pass;

import okhttp3.mockwebserver.MockWebServer;

final class EmptyEntityTest {

    //    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private MockWebServer server;
    private final ExecutorService executor = newSingleThreadExecutor();

    @AfterEach
    void shutDownExecutor() {
        executor.shutdown();
    }

    @BeforeEach
    void startMockServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void shutdownDriver() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldPassEmptyEntity() throws InterruptedException {
        server.enqueue(new MockResponse());

        var http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(server.url("/").uri())
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

        var request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("true", request.getHeader("Passed"));
    }

    @Test
    void shouldPassNonEmptyEntity() throws InterruptedException {
        server.enqueue(new MockResponse());

        var http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(server.url("/").uri())
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

        var request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("true", request.getHeader("Passed"));
    }

    @Test
    void shouldPassExplicitNonEmptyEntity() throws InterruptedException {
        server.enqueue(new MockResponse());

        var http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(server.url("/").uri())
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
                .body(message -> {
                })
                .call(pass())
                .join();

        var request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("true", request.getHeader("Passed"));
    }


}
