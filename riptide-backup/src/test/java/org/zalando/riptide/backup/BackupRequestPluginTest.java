package org.zalando.riptide.backup;

import com.github.restdriver.clientdriver.ClientDriverRule;
import com.google.common.collect.ImmutableMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.capture.Completion;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.PUT;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public final class BackupRequestPluginTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor(Executors.newFixedThreadPool(2));
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .plugin(new BackupRequestPlugin(newSingleThreadScheduledExecutor(), 1, SECONDS, executor))
            .build();

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldNotSendBackupRequestIfFastEnough() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    public void shouldUseBackupRequest() throws Throwable {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldUseOriginalRequest() throws Throwable {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(3, SECONDS));

        unit.get("/bar")
                .call(pass())
                .get(3, SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldUseFailedBackupRequest() {
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().withStatus(503));

        Completion.join(unit.get("/bar")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        on(SERVER_ERROR).call(() -> {
                            throw new IllegalStateException();
                        })));
    }

    @Test
    public void shouldNotSendBackupRequestForNonSafeRequests() {
        driver.addExpectation(onRequestTo("/baz").withMethod(PUT), giveEmptyResponse().after(2, SECONDS));

        unit.put("/baz")
                .call(pass())
                .join();
    }

    @Test
    public void shouldNotSendBackupRequestForGetWithBodyWithoutOverride() {
        driver.addExpectation(onRequestTo("/baz").withMethod(POST), giveEmptyResponse().after(2, SECONDS));

        unit.post("/baz")
                .call(pass())
                .join();
    }

    @Test
    public void shouldSendBackupRequestsForGetWithBody() throws Throwable {
        driver.addExpectation(onRequestTo("/bar").withMethod(POST), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar").withMethod(POST), giveEmptyResponse());

        unit.post("/bar")
                .header("X-HTTP-Method-Override", "GET")
                .body(ImmutableMap.of())
                .call(pass())
                .join();
    }

    @Test
    public void shouldSendBackupRequestForCustomSafeDetectedRequest() throws Throwable {
        final Http unit = Http.builder()
                .baseUrl(driver.getBaseUrl())
                .requestFactory(factory)
                .plugin(new BackupRequestPlugin(newSingleThreadScheduledExecutor(), 1, SECONDS, executor)
                        .withSafeMethodDetector(
                                arguments -> arguments.getHeaders().containsEntry("Allow-Backup-Request", "true")))
                .build();

        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse().after(2, SECONDS));
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .header("Allow-Backup-Request", "true")
                .call(pass())
                .get(1500, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldCancelRequests() throws InterruptedException {
        // TODO: support proper cancellations and remove this expectation
        driver.addExpectation(onRequestTo("/bar"), giveEmptyResponse());

        unit.get("/bar")
                .call(pass())
                .cancel(true);

        Thread.sleep(1000);
    }

}
