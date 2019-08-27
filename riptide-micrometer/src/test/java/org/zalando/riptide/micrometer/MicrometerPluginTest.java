package org.zalando.riptide.micrometer;

import com.fasterxml.jackson.databind.*;
import com.github.restdriver.clientdriver.*;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.*;
import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.json.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.net.*;
import java.util.concurrent.*;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.*;
import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static java.util.concurrent.TimeUnit.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.Series.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

final class MicrometerPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .converter(createJsonConverter())
            .plugin(new MicrometerPlugin(registry)
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

    MicrometerPluginTest() {
        this.factory.setReadTimeout(500);
    }

    @Test
    void shouldRecordSuccessResponseMetric() {
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
    void shouldRecordErrorResponseMetric() {
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

    @Test
    void shouldNotRecordFailureMetric() {
        driver.addExpectation(onRequestTo("/err"),
                giveEmptyResponse().after(750, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/err").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));

        @Nullable final Timer timer = registry.find("http.outgoing-requests").timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("method"), is("GET"));
        assertThat(timer.getId().getTag("uri"), is("/err"));
        assertThat(timer.getId().getTag("status"), is("CLIENT_ERROR"));
        assertThat(timer.getId().getTag("clientName"), is("localhost"));
        assertThat(timer.getId().getTag("client"), is("example"));
    }

}
