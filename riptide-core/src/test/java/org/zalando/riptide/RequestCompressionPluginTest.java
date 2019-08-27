package org.zalando.riptide;

import com.github.restdriver.clientdriver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;

import java.util.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static org.hamcrest.Matchers.*;
import static org.zalando.riptide.PassRoute.*;

final class RequestCompressionPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http unit = Http.builder()
            .executor(executor)
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .plugin(new RequestCompressionPlugin())
            .build();

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void shouldNotCompressEmptyBody() {
        driver.addExpectation(onRequestTo("/")
                        .withBody(emptyString(), "text/plain")
                        .withoutHeader("Content-Encoding"),
                giveResponse("", "text/plain"));

        unit.get("/")
                .contentType(MediaType.TEXT_PLAIN)
                .call(pass())
                .join();
    }

    @Test
    void shouldCompressNonEmptyBody() {
        driver.addExpectation(onRequestTo("/")
                        .withMethod(POST)
                        .withBody(not(equalTo("{}")), "application/json")
                        .withHeader("Content-Encoding", "gzip"),
                giveResponse("", "text/plain"));

        unit.post("/")
                .body(new HashMap<>())
                .call(pass())
                .join();
    }

}
