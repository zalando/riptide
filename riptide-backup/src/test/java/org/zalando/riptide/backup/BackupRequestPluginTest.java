package org.zalando.riptide.backup;

import com.github.restdriver.clientdriver.*;
import com.google.common.collect.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.Collections.*;
import static java.util.concurrent.Executors.*;
import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

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
        driver.verify();
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
    void shouldNotSendBackupRequestForNonIdempotentRequests() {
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

}
