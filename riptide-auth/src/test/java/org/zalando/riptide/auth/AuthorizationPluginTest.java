package org.zalando.riptide.auth;

import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static org.zalando.riptide.PassRoute.*;

final class AuthorizationPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new AuthorizationPlugin(() -> "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30."))
            .build();

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @AfterEach
    void verify() {
        driver.verify();
    }

    @Test
    void shouldFail() {
        driver.addExpectation(
                onRequestTo("/").withHeader("Authorization", "Bearer eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30."),
                giveEmptyResponse());

        http.get("/")
                .call(pass())
                .join();
    }

    @Test
    void shouldNotOverwriteAuthorizationHeader() {
        driver.addExpectation(
                onRequestTo("/").withHeader("Authorization", "Basic dXNlcjpzZWNyZXQK"),
                giveEmptyResponse());

        http.get("/")
                .header("Authorization", "Basic dXNlcjpzZWNyZXQK")
                .call(pass())
                .join();
    }

}
