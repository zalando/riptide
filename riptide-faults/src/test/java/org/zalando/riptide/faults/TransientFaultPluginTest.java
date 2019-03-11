package org.zalando.riptide.faults;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class TransientFaultPluginTest {

    private static final int SOCKET_TIMEOUT = 1000;
    private static final int DELAY = 2000;

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .build())
            .build();

    private final ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void shouldClassifyAsTransient() {
        final Http unit = newUnit(new TransientFaultPlugin());

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().after(DELAY, TimeUnit.MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class, () -> execute(unit));

        assertThat(exception.getCause(), is(instanceOf(TransientFaultException.class)));
        assertThat(exception.getCause().getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldNotClassifyAsTransientIfNotMatching() {
        final Http unit = newUnit(new TransientFaultPlugin(new DefaultFaultClassifier()
                .exclude(SocketTimeoutException.class::isInstance)));

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().after(DELAY, TimeUnit.MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class, () -> execute(unit));
        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
    }

    @Test
    void shouldClassifyExceptionAsTransientAsIs() {
        final Http unit = newUnit(new Plugin() {
            @Override
            public RequestExecution aroundNetwork(final RequestExecution execution) {
                return arguments -> {
                    final CompletableFuture<ClientHttpResponse> future = new CompletableFuture<>();
                    future.completeExceptionally(new IllegalArgumentException());
                    return future;
                };
            }
        }, new TransientFaultPlugin(new DefaultFaultClassifier().include(IllegalArgumentException.class::isInstance)));

        final CompletionException exception = assertThrows(CompletionException.class, request(unit)::join);

        assertThat(exception.getCause(), is(instanceOf(TransientFaultException.class)));
        assertThat(exception.getCause().getCause(), is(instanceOf(IllegalArgumentException.class)));
    }

    private void execute(final Http unit) {
        request(unit)
                .join();
    }

    private CompletableFuture<ClientHttpResponse> request(final Http unit) {
        return unit.get("/")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    void shouldClassifyAsPermanent() {
        final Http unit = newUnit(new TransientFaultPlugin());

        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse());

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/")
                        .dispatch(series(),
                                on(SUCCESSFUL).call(() -> {
                                    throw new MalformedURLException();
                                }))::join);

        assertThat(exception.getCause(), is(instanceOf(MalformedURLException.class)));
    }

    private Http newUnit(final Plugin... plugins) {
        return Http.builder()
                .executor(executor)
                .requestFactory(factory)
                .baseUrl(driver.getBaseUrl())
                .plugins(Arrays.asList(plugins))
                .build();
    }

}
