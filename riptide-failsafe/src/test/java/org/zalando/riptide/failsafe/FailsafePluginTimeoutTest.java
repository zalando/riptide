package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.failsafe.Timeout;
import dev.failsafe.TimeoutExceededException;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.MockWebServerUtil.*;

final class FailsafePluginTimeoutTest {

    private final MockWebServer server = new MockWebServer();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final Http unit = Http.builder()
            .executor(newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(getBaseUrl(server))
            .converter(createJsonConverter())
            .plugin(new FailsafePlugin()
                    .withPolicy(Timeout.of(Duration.ofSeconds(1))))
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
        server.shutdown();
    }

    @Test
    void shouldNotTimeout() {
        server.enqueue(emptyMockResponse());

        unit.get("/foo")
                .call(pass())
                .join();
        verify(server, 1, "/foo");
    }

    @Test
    void shouldTimeout() {
        server.enqueue(emptyMockResponse().setHeadersDelay(2, SECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo")
                        .call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(TimeoutExceededException.class)));
        verify(server, 1, "/foo");
    }

}
