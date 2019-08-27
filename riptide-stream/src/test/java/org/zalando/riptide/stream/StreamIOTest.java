package org.zalando.riptide.stream;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static com.google.common.io.Resources.*;
import static java.util.Collections.*;
import static java.util.concurrent.Executors.*;
import static java.util.stream.Collectors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.stream.Streams.*;

final class StreamIOTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

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
            .baseUrl(driver.getBaseUrl())
            .converter(streamConverter(new ObjectMapper().disable(FAIL_ON_UNKNOWN_PROPERTIES),
                    singletonList(APPLICATION_JSON)))
            .build();

    @AfterEach
    void shutdownExecutor() {
        executor.shutdown();
    }

    @Test
    void shouldReadContributors() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final AtomicReference<Stream<User>> reference = new AtomicReference<>();

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(streamOf(User.class), reference::set)).join();

        final List<String> users = reference.get()
                .map(User::getLogin)
                .collect(toList());

        assertThat(users, hasItems("jhorstmann", "lukasniemeier-zalando", "whiskeysierra"));
    }

    @Test
    void shouldCancelRequest() throws IOException {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveResponseAsBytes(getResource("contributors.json").openStream(), "application/json"));

        final CompletableFuture<ClientHttpResponse> future = http.get("/repos/{org}/{repo}/contributors", "zalando",
                "riptide")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));

        future.cancel(true);

        assertThrows(CancellationException.class, future::join);
    }

    @Test
    void shouldReadEmptyResponse() {
        driver.addExpectation(onRequestTo("/repos/zalando/riptide/contributors"),
                giveEmptyResponse().withStatus(200));

        http.get("/repos/{org}/{repo}/contributors", "zalando", "riptide")
                .dispatch(reasonPhrase(),
                        on("OK").call(ClientHttpResponse::close)).join();
    }

}
