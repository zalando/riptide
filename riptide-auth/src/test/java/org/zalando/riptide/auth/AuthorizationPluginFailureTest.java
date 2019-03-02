package org.zalando.riptide.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;

import java.nio.file.NoSuchFileException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.PassRoute.pass;

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
