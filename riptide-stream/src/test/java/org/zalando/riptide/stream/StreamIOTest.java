package org.zalando.riptide.stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.reasonPhrase;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.stream.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.stream.MockWebServerUtil.jsonMockResponseFromResource;
import static org.zalando.riptide.stream.MockWebServerUtil.verify;
import static org.zalando.riptide.stream.Streams.streamConverter;
import static org.zalando.riptide.stream.Streams.streamOf;

final class StreamIOTest {

    private final MockWebServer server = new MockWebServer();

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        String getLogin() {
            return login;
        }
    }

    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(getBaseUrl(server))
            .converter(streamConverter(new JsonMapper(),
                    singletonList(APPLICATION_JSON)))
            .build();

    @SneakyThrows
    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
        server.shutdown();
    }

    @Test
    void shouldReadContributors() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final AtomicReference<Stream<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(streamOf(User.class), reference::set)).join();

        final List<String> users = reference.get()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
        verify(server, 1, "/repos/zalando/riptide/contributors");
    }

    @Test
    void shouldCancelRequest() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final CompletableFuture<ClientHttpResponse> future = http.get("/repos/{org}/{repo}/contributors", "zalando",
                "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        future.cancel(true);

        assertThrows(CancellationException.class, future::join);
        verify(server, 0, "");
    }

    @Test
    void shouldReadEmptyResponse() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();

        verify(server, 1, "/repos/zalando/riptide/contributors");

    }

}
