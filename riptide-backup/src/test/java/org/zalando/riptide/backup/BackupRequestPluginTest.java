package org.zalando.riptide.backup;

import com.github.restdriver.clientdriver.ClientDriverRule;
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
    public void shouldNotSendBackupRequestIfFastEnough() throws Throwable {
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
    public void shouldUseFailedBackupRequest() throws Throwable {
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
    public void shouldNotSendBackupRequestForNonIdempotentRequests() throws Throwable {
        driver.addExpectation(onRequestTo("/baz").withMethod(POST), giveEmptyResponse().after(2, SECONDS));

        unit.post("/baz")
                .call(pass())
                .join();
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
