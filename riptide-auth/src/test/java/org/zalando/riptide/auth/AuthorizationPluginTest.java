package org.zalando.riptide.auth;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;

import java.util.concurrent.ExecutorService;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.zalando.riptide.PassRoute.pass;

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
    void shouldAddAuthorizationHeader() {
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
