package org.zalando.riptide.timeout;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.*;
import org.zalando.riptide.*;
import org.zalando.riptide.httpclient.*;

import java.io.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.Executors.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.PassRoute.*;

final class TimeoutPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final CloseableHttpClient client = HttpClientBuilder.create().build();
    private final Executor executor = newFixedThreadPool(2);
    private final ApacheClientHttpRequestFactory factory = new ApacheClientHttpRequestFactory(client);

    private final Http unit = Http.builder()
            .executor(executor)
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
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

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @Test
    void shouldNotTimeout() {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse());

        unit.get("/foo")
                .call(pass())
                .join();
    }

    @Test
    void shouldTimeout() {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().after(2, TimeUnit.SECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/foo")
                        .call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(TimeoutException.class)));
    }

}
