package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.mock;
import static org.zalando.fauxpas.FauxPas.partially;
import static org.zalando.riptide.PassRoute.pass;

public class FailsafePluginNoCircuitBreakerTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final RetryListener listeners = mock(RetryListener.class);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(new RestAsyncClientHttpRequestFactory(client,
                    new ConcurrentTaskExecutor(newSingleThreadExecutor())))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(newSingleThreadScheduledExecutor())
                    .withListener(listeners))
            .plugin(new OriginalStackTracePlugin())
            .build();

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(createObjectMapper());
        return converter;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @After
    public void tearDown() throws IOException {
        client.close();
    }

    @Test
    public void shouldNotOpenCircuit() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo").call(pass())
                .exceptionally(this::ignore)
                .join();

        final CompletableFuture<Void> timeout = unit.get("/foo").call(pass());
        final CompletableFuture<Void> last = unit.get("/foo").call(pass());

        timeout.exceptionally(partially(SocketTimeoutException.class, this::ignore)).join();
        last.join();
    }

    private Void ignore(@SuppressWarnings("unused") final Throwable throwable) {
        return null;
    }

}
