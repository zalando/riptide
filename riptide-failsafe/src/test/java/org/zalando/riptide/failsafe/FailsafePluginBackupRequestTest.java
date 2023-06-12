package org.zalando.riptide.failsafe;

import com.google.common.collect.ImmutableMap;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.*;

final class FailsafePluginBackupRequestTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final Executor executor = newFixedThreadPool(2);
    private final ClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final Http unit = Http.builder()
            .executor(executor)
            .requestFactory(factory)
            .baseUrl(getBaseUrl(server))
            .plugin(new FailsafePlugin()
                .withPolicy(new BackupRequest<>(1, SECONDS)))
            .build();

    @AfterEach
    void tearDown() throws IOException {
        client.close();
        server.shutdown();
    }

    @Test
    void shouldNotSendBackupRequestIfFastEnough() {
        server.enqueue(emptyMockResponse());
        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    void shouldUseBackupRequest() throws Throwable {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(emptyMockResponse());

        unit.get("/bar")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);

        verify(server, 2, "/bar");
    }

    @Test
    void shouldUseOriginalRequest() throws Throwable {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(emptyMockResponse().setHeadersDelay(3, SECONDS));


        unit.get("/bar")
                .call(pass())
                .get(3, SECONDS);

        verify(server, 2, "/bar");
    }

    @Test
    void shouldUseFailedBackupRequest() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(new MockResponse().setResponseCode(SERVICE_UNAVAILABLE.value()));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get("/bar")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()),
                                on(SERVER_ERROR).call(() -> {
                                    throw new IllegalStateException();
                                }))
                        .join());

        assertEquals(IllegalStateException.class, exception.getCause().getClass());

        verify(server, 2, "/bar");
    }

    @Test
    void shouldNotSendBackupRequestForNonIdempotentRequests() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));

        unit.post("/baz")
                .call(pass())
                .join();

        verify(server, 1, "/baz", POST.toString());
    }

    @Test
    void shouldSendBackupRequestsForGetWithBody() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(emptyMockResponse());

        unit.post("/bar")
                .header("X-HTTP-Method-Override", "GET")
                .body(ImmutableMap.of())
                .call(pass())
                .join();

        verify(server, 2, "/bar", POST.toString());
    }

    @Test
    void shouldSendBackupRequestForCustomSafeDetectedRequest() throws Throwable {
        final Http unit = Http.builder()
                .executor(executor)
                .requestFactory(factory)
                .baseUrl(getBaseUrl(server))
                .plugin(new FailsafePlugin()
                    .withPolicy(new BackupRequest<>(1, SECONDS), arguments ->
                        arguments.getHeaders()
                                .getOrDefault("Allow-Backup-Request", emptyList()).contains("true")))
                .build();

        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));
        server.enqueue(emptyMockResponse());

        unit.get("/bar")
                .header("Allow-Backup-Request", "true")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);

        verify(server, 2, "/bar");
    }

}
