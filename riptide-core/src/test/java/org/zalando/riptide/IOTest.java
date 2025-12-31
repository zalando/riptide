package org.zalando.riptide;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.MockWebServerUtil.jsonMockResponseFromResource;
import static org.zalando.riptide.MockWebServerUtil.verify;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Types.listOf;

final class IOTest {

    private final MockWebServer server = new MockWebServer();

    record User(String login) {
    }

    private final ExecutorService executor = newSingleThreadExecutor();

    private final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(requestFactory)
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .build();

    private static JacksonJsonHttpMessageConverter createJsonConverter() {
        return new JacksonJsonHttpMessageConverter();
    }

    @SneakyThrows
    @AfterEach
    void tearDown() {
        executor.shutdown();
        server.shutdown();
    }

    @Test
    void shouldBuffer() throws IOException {
        //requestFactory.setBufferRequestBody(true);
        shouldReadContributors();
    }

    @Test
    void shouldStream() throws IOException {
        //requestFactory.setBufferRequestBody(false);
        shouldReadContributors();
    }

    private void shouldReadContributors() throws IOException {
        server.enqueue(jsonMockResponseFromResource("contributors.json"));

        final AtomicReference<List<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), reference::set)).join();

        final List<String> users = reference.get().stream()
                .map(User::login)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
        verify(server, 1, "/repos/zalando/riptide/contributors");
    }

}
