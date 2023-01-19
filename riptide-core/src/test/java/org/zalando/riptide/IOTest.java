package org.zalando.riptide;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Types.listOf;

final class IOTest {

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        String getLogin() {
            return login;
        }
    }

    private final ExecutorService executor = newSingleThreadExecutor();

    private final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        return converter;
    }

    private MockWebServer mockWebServer;
    private Http http;

    @BeforeEach
    void setup() throws IOException {
         mockWebServer = new MockWebServer();
         mockWebServer.enqueue(new MockResponse()
                 .setHeader("Content-Type","application/json")
                 .setBody(Resources.toString(getResource("contributors.json"), StandardCharsets.UTF_8)));
         mockWebServer.start();
         http = Http.builder()
                .executor(executor)
                .requestFactory(requestFactory)
                .baseUrl(mockWebServer.url("/").uri())
                .converter(createJsonConverter())
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        executor.shutdown();
    }

    @Test
    void shouldBuffer() throws IOException {
        requestFactory.setBufferRequestBody(true);
        shouldReadContributors();
    }

    @Test
    void shouldStream() throws IOException {
        requestFactory.setBufferRequestBody(false);
        shouldReadContributors();
    }

    private void shouldReadContributors() throws IOException {

        final AtomicReference<List<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(listOf(User.class), reference::set)).join();

        final List<String> users = reference.get().stream()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }
}
