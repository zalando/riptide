package org.zalando.riptide.faults;

import com.github.restdriver.clientdriver.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class TransientFaultPluginTest {

    private static final int SOCKET_TIMEOUT = 1000;
    private static final int DELAY = 2000;

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(SOCKET_TIMEOUT)
                    .build())
            .build();

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
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(factory)
                .baseUrl(driver.getBaseUrl())
                .plugins(Arrays.asList(plugins))
                .build();
    }

}
