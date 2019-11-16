package org.zalando.riptide;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.equalTo;
import static org.zalando.riptide.PassRoute.pass;

class NettyClientIntegrationTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();
    private final ExecutorService executor = newSingleThreadExecutor();

    private final Http http = Http.builder()
            .executor(executor)
            .requestFactory(new Netty4ClientHttpRequestFactory())
            .baseUrl(driver.getBaseUrl())
            .build();

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        driver.verify();
    }

    @Test
    void shouldSendBodyWithNettyClient() {
        driver.addExpectation(onRequestTo("/")
                        .withMethod(POST)
                        .withBody(equalTo("{}"), "application/json"),
                giveResponse("", "text/plain"));

        http.post("/")
                .body(new HashMap<>())
                .call(pass())
                .join();
    }
}
