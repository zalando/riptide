package org.zalando.riptide.backup;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import com.google.common.collect.ImmutableMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.PUT;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.emptyList;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class BackupRequestPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final Executor executor = newFixedThreadPool(2);
    private final ClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final Http unit = Http.builder()
            .executor(executor)
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .plugin(new BackupRequestPlugin(newSingleThreadScheduledExecutor(), 1, SECONDS).withExecutor(executor))
            .build();

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void shouldNotSendBackupRequestIfFastEnough() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    void shouldUseBackupRequest() throws Throwable {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldUseOriginalRequest() throws Throwable {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(3, SECONDS));

        unit.get("/bar")
                .call(pass())
                .get(3, SECONDS);
    }

    @Test
    void shouldUseFailedBackupRequest() {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().withStatus(503));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get("/bar")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(pass()),
                                on(SERVER_ERROR).call(() -> {
                                    throw new IllegalStateException();
                                }))
                        .join());

        assertEquals(IllegalStateException.class, exception.getCause().getClass());
    }

    @Test
    void shouldNotSendBackupRequestForNonSafeRequests() {
        driver.addExpectation(onRequestTo("/baz").withMethod(PUT), giveEmptyResponse().after(2, SECONDS));

        unit.put("/baz")
                .call(pass())
                .join();
    }

    @Test
    void shouldNotSendBackupRequestForGetWithBodyWithoutOverride() {
        driver.addExpectation(onRequestTo("/baz").withMethod(POST), giveEmptyResponse().after(2, SECONDS));

        unit.post("/baz")
                .call(pass())
                .join();
    }

    @Test
    void shouldSendBackupRequestsForGetWithBody() {
        driver.addExpectation(onRequestTo("/bar").withMethod(POST), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar").withMethod(POST), giveEmptyResponse());

        unit.post("/bar")
                .header("X-HTTP-Method-Override", "GET")
                .body(ImmutableMap.of())
                .call(pass())
                .join();
    }

    @Test
    void shouldSendBackupRequestForCustomSafeDetectedRequest() throws Throwable {
        final Http unit = Http.builder()
                .executor(executor)
                .requestFactory(factory)
                .baseUrl(driver.getBaseUrl())
                .plugin(new BackupRequestPlugin(newSingleThreadScheduledExecutor(), 1, SECONDS)
                        .withPredicate(arguments ->
                                arguments.getHeaders()
                                        .getOrDefault("Allow-Backup-Request", emptyList()).contains("true"))
                        .withExecutor(executor))
                .build();

        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .header("Allow-Backup-Request", "true")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);
    }

    @Test
    void shouldCancelRequests() throws InterruptedException {
        // TODO: support proper cancellations and remove this expectation
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .call(pass())
                .cancel(true);

        Thread.sleep(1000);
    }

}
