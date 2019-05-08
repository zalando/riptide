package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.concurrent.ExecutorService;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zalando.riptide.PassRoute.pass;

final class EmptyEntityTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final ExecutorService executor = newSingleThreadExecutor();

    @AfterEach
    void shutDownExecutor() {
        executor.shutdown();
    }

    @AfterEach
    void shutdownDriver() {
        driver.shutdown();
    }

    @Test
    void shouldPassEmptyEntity() {
        driver.addExpectation(onRequestTo("/").withHeader("Passed", "true"),
                giveEmptyResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(driver.getBaseUrl())
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertTrue(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.get("/")
                .call(pass())
                .join();

        driver.verify();
    }

    @Test
    void shouldPassNonEmptyEntity() {
        driver.addExpectation(onRequestTo("/").withMethod(POST).withHeader("Passed", "true"),
                giveEmptyResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(driver.getBaseUrl())
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertFalse(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.post("/")
                .body(emptyMap())
                .call(pass())
                .join();

        driver.verify();
    }

    @Test
    void shouldPassExplicitNonEmptyEntity() {
        driver.addExpectation(onRequestTo("/").withMethod(POST).withHeader("Passed", "true"),
                giveEmptyResponse());

        final Http http = Http.builder()
                .executor(executor)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .baseUrl(driver.getBaseUrl())
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            assertFalse(arguments.getEntity().isEmpty());
                            return execution.execute(arguments.withHeader("Passed", "true"));
                        };
                    }
                })
                .build();

        http.post("/")
                .body(message -> {})
                .call(pass())
                .join();

        driver.verify();
    }


}
