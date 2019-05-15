package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.zalando.riptide.PassRoute.pass;

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
