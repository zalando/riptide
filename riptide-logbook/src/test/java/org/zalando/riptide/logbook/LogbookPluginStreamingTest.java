package org.zalando.riptide.logbook;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.core.DefaultSink;
import org.zalando.logbook.json.JsonHttpLogFormatter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.logbook.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.logbook.MockWebServerUtil.textMockResponse;

final class LogbookPluginStreamingTest {
    private final MockWebServer server = new MockWebServer();

    private final ExecutorService executor = newSingleThreadExecutor();

    private final HttpLogWriter writer = mock(HttpLogWriter.class);

    private final Logbook logbook = Logbook.builder()
            .sink(new DefaultSink(new JsonHttpLogFormatter(), writer))
            .build();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new ApacheClientHttpRequestFactory(HttpClients.createDefault()))
            .plugin(new LogbookPlugin(logbook))
            .baseUrl(getBaseUrl(server))
            .build();

    @BeforeEach
    void defaultBehavior() {
        when(writer.isActive()).thenCallRealMethod();
    }

    @AfterEach
    @SneakyThrows
    void shutdownServer() {
        server.shutdown();
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
    }

    @Test
    void shouldLog() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        http.get("/")
                .call(pass())
                .join();


        final String request = request();
        assertThat(request, containsString("\"type\":\"request\""));
        assertThat(request, containsString("\"origin\":\"local\""));
        assertThat(request, containsString("\"method\":\"GET\""));
        assertThat(request, containsString("/"));

        final String response = response();
        assertThat(response, containsString("\"type\":\"response\""));
        assertThat(response, containsString("\"origin\":\"remote\""));

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldLogWithBody() throws IOException {
        server.enqueue(textMockResponse("World!"));

        http.post("/greet")
                .contentType(MediaType.TEXT_PLAIN)
                .body("Hello?")
                .call(pass())
                .join();

        final String request = request();
        assertThat(request, containsString("\"type\":\"request\""));
        assertThat(request, containsString("\"origin\":\"local\""));
        assertThat(request, containsString("\"method\":\"POST\""));
        assertThat(request, containsString("/greet"));
        assertThat(request, containsString("text/plain"));
        assertThat(request, containsString("\"body\":\"Hello?\""));

        final String response = response();
        assertThat(response, containsString("\"type\":\"response\""));
        assertThat(response, containsString("\"origin\":\"remote\""));
        assertThat(response, containsString("text/plain"));
        assertThat(response, containsString("\"body\":\"World!\""));

        MockWebServerUtil.verify(server, 1, "/greet", POST.toString());
    }

    /**
     * Tests that port 80 is supported:
     */
    @Test
    void shouldLogLocalhost() {
        final CompletableFuture<ClientHttpResponse> future = http.post("http://localhost")
                .call(pass());

        assertThrows(CompletionException.class, future::join);
    }

    private String request() throws IOException {
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(any(Precorrelation.class), captor.capture());
        return captor.getValue();
    }

    private String response() throws IOException {
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(writer).write(any(Correlation.class), captor.capture());
        return captor.getValue();
    }

}
