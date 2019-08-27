package org.zalando.riptide.auth;

import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.nio.file.*;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.PassRoute.*;

final class AuthorizationPluginFailureTest {

    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl("http://localhost")
            .plugin(new AuthorizationPlugin(new PlatformCredentialsAuthorizationProvider("missing")))
            .build();

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void shouldFailOnMissingFile() {
        final CompletableFuture<ClientHttpResponse> future = http.get("/").call(pass());
        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(NoSuchFileException.class)));
    }

}
