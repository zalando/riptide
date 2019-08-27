package org.zalando.riptide.logbook;

import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.zalando.logbook.*;
import org.zalando.logbook.json.*;
import org.zalando.riptide.*;

import java.io.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zalando.riptide.PassRoute.*;

final class LogbookPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final ExecutorService executor = newSingleThreadExecutor();

    private final HttpLogWriter writer = mock(HttpLogWriter.class);

    private final Logbook logbook = Logbook.builder()
            .sink(new DefaultSink(new JsonHttpLogFormatter(), writer))
            .build();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .plugin(new RequestCompressionPlugin())
            .plugin(new LogbookPlugin(logbook))
            .baseUrl(driver.getBaseUrl())
            .build();

    @AfterEach
    void shutdownDriver() {
        driver.shutdown();
    }

    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
    }

    @BeforeEach
    void defaultBehavior() {
        when(writer.isActive()).thenCallRealMethod();
    }

    @Test
    void shouldLog() throws IOException {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withStatus(200));

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
    }

    @Test
    void shouldLogWithBody() throws IOException {
        driver.addExpectation(onRequestTo("/greet")
                        .withMethod(POST)
                        .withBody(notNullValue(String.class), "text/plain"),
                giveResponse("World!", "text/plain"));

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
