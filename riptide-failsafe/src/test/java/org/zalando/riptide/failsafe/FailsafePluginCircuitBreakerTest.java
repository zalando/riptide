package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import com.google.common.collect.*;
import net.jodah.failsafe.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zalando.fauxpas.FauxPas.*;
import static org.zalando.riptide.PassRoute.*;

final class FailsafePluginCircuitBreakerTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final RetryListener listeners = mock(RetryListener.class);

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(client))
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin(
                    ImmutableList.of(new CircuitBreaker<ClientHttpResponse>()
                            .withDelay(Duration.ofSeconds(1))),
                    newSingleThreadScheduledExecutor())
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

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void shouldOpenCircuit() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));

        unit.get("/foo").call(pass())
                .exceptionally(partially(SocketTimeoutException.class, this::ignore))
                .join();

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(CircuitBreakerOpenException.class)));
    }

    private ClientHttpResponse ignore(@SuppressWarnings("unused") final Throwable throwable) {
        return null;
    }

}
