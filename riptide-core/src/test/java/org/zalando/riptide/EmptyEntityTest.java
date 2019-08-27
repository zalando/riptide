package org.zalando.riptide;

import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;

import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.Collections.*;
import static java.util.concurrent.Executors.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.PassRoute.*;

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
