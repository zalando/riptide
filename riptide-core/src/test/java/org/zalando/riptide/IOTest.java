package org.zalando.riptide;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static com.google.common.io.Resources.*;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.Types.*;

final class IOTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    @JsonAutoDetect(fieldVisibility = NON_PRIVATE)
    static class User {
        String login;

        String getLogin() {
            return login;
        }
    }

    private final ExecutorService executor = newSingleThreadExecutor();

    private final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(requestFactory)
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .build();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        return converter;
    }

    @AfterEach
    void tearDown() {
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
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

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
