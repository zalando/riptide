package org.zalando.riptide.exceptions;

import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.Rest;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public final class TemporaryExceptionPluginTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setSocketTimeout(500)
                .build())
            .build();

    private final ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldClassifyAsTemporary() {
        final Rest unit = newUnit(new TemporaryExceptionPlugin());

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().after(1000, TimeUnit.MILLISECONDS));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(TemporaryException.class));
        // not second level of CompletionException!
        exception.expectCause(hasFeature(Throwable::getCause, is(instanceOf(SocketTimeoutException.class))));

        execute(unit);
    }

    @Test
    public void shouldNotClassifyAsTemporaryIfNotMatching() {
        final Rest unit = newUnit(new TemporaryExceptionPlugin(emptyList()));

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().after(1000, TimeUnit.MILLISECONDS));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(SocketTimeoutException.class));

        execute(unit);
    }

    @Test
    public void shouldClassifyExceptionAsTemporaryAsIs() {
        final Rest unit = newUnit((arguments, execution) -> () -> {
                    final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(new IllegalArgumentException());
                    return future;
                }, new TemporaryExceptionPlugin(IllegalArgumentException.class::isInstance));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(TemporaryException.class));
        exception.expectCause(hasFeature(Throwable::getCause, is(instanceOf(IllegalArgumentException.class))));

        request(unit).join();
    }

    private void execute(final Rest unit) {
        request(unit)
                .join();
    }

    private CompletableFuture<Void> request(final Rest unit) {
        return unit.get("/")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldClassifyAsPermanent() {
        final Rest unit = newUnit(new TemporaryExceptionPlugin());

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse());

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(MalformedURLException.class));

        unit.get("/")
                .dispatch(series(),
                        on(SUCCESSFUL).call(() -> {throw new MalformedURLException();}))
                .join();
    }

    private Rest newUnit(final Plugin... plugins) {
        return Rest.builder()
                .baseUrl(driver.getBaseUrl())
                .requestFactory(factory)
                .plugins(Arrays.asList(plugins))
                .build();
    }

}