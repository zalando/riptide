package org.zalando.riptide.metrics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.hamcrest.MockitoHamcrest.doubleThat;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

public class MetricsPluginTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();
    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final GaugeService gaugeService = spy(GaugeService.class);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .converter(createJsonConverter())
            .plugin(new MetricsPlugin(gaugeService, (arguments, response) -> arguments.getMethod().name()))
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
    public void shouldRecordSuccessMetric() throws Throwable {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();

        Thread.sleep(1000); // because the future won't wait for metrics

        verify(gaugeService).submit(argThat(equalTo("GET")), doubleThat(is(greaterThan(0d))));
    }

    @Test
    public void shouldRecordFailureMetric() throws Throwable {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().withStatus(503));

        unit.get("/foo")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .exceptionally(e -> null)
                .join();

        Thread.sleep(1000); // because the future won't wait for metrics

        verify(gaugeService).submit(argThat(equalTo("GET")), doubleThat(is(greaterThan(0d))));
    }

    @Test(expected = SocketTimeoutException.class)
    public void shouldNotRecordMetric() throws Throwable {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().after(750, TimeUnit.MILLISECONDS));

        try {
            unit.get("/foo")
                    .call(pass())
                    .join();

            fail("Expected exception");
        } catch (final CompletionException e) {
            throw e.getCause();
        } finally {
            verifyZeroInteractions(gaugeService);
        }
    }

}
