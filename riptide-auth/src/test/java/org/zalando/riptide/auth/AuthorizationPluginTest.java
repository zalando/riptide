package org.zalando.riptide.auth;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.auth.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.auth.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.auth.MockWebServerUtil.verify;

final class AuthorizationPluginTest {

    private final MockWebServer server = new MockWebServer();

    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .plugin(new AuthorizationPlugin(() -> "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30."))
            .build();

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void shouldFail() {
        server.enqueue(emptyMockResponse());

        http.get("/")
                .call(pass())
                .join();

        verify(server, 1, "/", headers -> {
            assertEquals("Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.", headers.get("Authorization"));
        });
    }

    @Test
    void shouldNotOverwriteAuthorizationHeader() {
        server.enqueue(emptyMockResponse());

        http.get("/")
                .header("Authorization", "Basic dXNlcjpzZWNyZXQK")
                .call(pass())
                .join();

        verify(server, 1, "/", headers -> {
            assertEquals("Basic dXNlcjpzZWNyZXQK", headers.get("Authorization"));
        });
    }

}
