package org.zalando.riptide.metrics;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletionException;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
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

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final Http unit = Http.builder()
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new MetricsPlugin(registry)
                    .withMetricName("http.outgoing-requests")
                    .withDefaultTags(Tag.of("client", "example")))
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
    public void shouldRecordSuccessResponseMetric() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().withStatus(200));

        unit.get("/foo")
                .call(pass())
                .join();

        @Nullable final Timer timer = registry.find("http.outgoing-requests").timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("method"), is("GET"));
        assertThat(timer.getId().getTag("uri"), is("/foo"));
        assertThat(timer.getId().getTag("status"), is("200"));
        assertThat(timer.getId().getTag("clientName"), is("localhost"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
    }

    @Test
    public void shouldRecordErrorResponseMetric() {
        driver.addExpectation(onRequestTo("/bar").withMethod(POST),
                giveEmptyResponse().withStatus(503));

        unit.post("/bar")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .exceptionally(e -> null)
                .join();

        @Nullable final Timer timer = registry.find("http.outgoing-requests").timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("method"), is("POST"));
        assertThat(timer.getId().getTag("uri"), is("/bar"));
        assertThat(timer.getId().getTag("status"), is("503"));
        assertThat(timer.getId().getTag("clientName"), is("localhost"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
    }

    @Test(expected = SocketTimeoutException.class)
    public void shouldNotRecordFailureMetric() throws Throwable {
        driver.addExpectation(onRequestTo("/err"),
                giveEmptyResponse().after(750, MILLISECONDS));

        try {
            unit.get("/err")
                    .call(pass())
                    .join();

            fail("Expected exception");
        } catch (final CompletionException e) {
            throw e.getCause();
        } finally {
            @Nullable final Timer timer = registry.find("http.outgoing-requests").timer();

            assertThat(timer, is(notNullValue()));
            assertThat(timer.getId().getTag("method"), is("GET"));
            assertThat(timer.getId().getTag("uri"), is("/err"));
            assertThat(timer.getId().getTag("status"), is("CLIENT_ERROR"));
            assertThat(timer.getId().getTag("clientName"), is("localhost"));
            assertThat(timer.getId().getTag("client"), is("example"));
        }
    }

}
