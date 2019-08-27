package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import com.google.common.collect.*;
import org.apache.http.client.config.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static java.util.concurrent.TimeUnit.*;
import static org.mockito.Mockito.*;
import static org.zalando.fauxpas.FauxPas.*;
import static org.zalando.riptide.PassRoute.*;

final class FailsafePluginNoPolicyTest {

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
            .plugin(new FailsafePlugin(ImmutableList.of(), newSingleThreadScheduledExecutor())
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
    void shouldNotOpenCircuit() {
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse().after(800, MILLISECONDS));
        driver.addExpectation(onRequestTo("/foo"), giveEmptyResponse());

        unit.get("/foo").call(pass())
                .exceptionally(this::ignore)
                .join();

        final CompletableFuture<ClientHttpResponse> timeout = unit.get("/foo").call(pass());
        final CompletableFuture<ClientHttpResponse> last = unit.get("/foo").call(pass());

        timeout.exceptionally(partially(SocketTimeoutException.class, this::ignore)).join();
        last.join();
    }

    private ClientHttpResponse ignore(@SuppressWarnings("unused") final Throwable throwable) {
        return null;
    }

}
