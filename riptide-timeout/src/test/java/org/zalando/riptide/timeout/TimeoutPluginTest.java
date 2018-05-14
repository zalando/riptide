package org.zalando.riptide.timeout;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.restdriver.clientdriver.ClientDriverRule;
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

import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.fail;
import static org.zalando.riptide.PassRoute.pass;

public class TimeoutPluginTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http unit = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .converter(createJsonConverter())
            .plugin(new TimeoutPlugin(newSingleThreadScheduledExecutor(), 1, TimeUnit.SECONDS, executor))
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
    public void shouldNotTimeout() throws Throwable {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test(expected = TimeoutException.class)
    public void shouldTimeout() throws Throwable {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().after(1, TimeUnit.SECONDS));

        try {
            unit.get("/foo")
                    .call(pass())
                    .join();
            fail("Expecting exception");
        } catch (final CompletionException e) {
            throw e.getCause();
        }
    }

}
