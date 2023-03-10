package org.zalando.riptide.micrometer;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import dev.failsafe.function.CheckedPredicate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import dev.failsafe.RetryPolicy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.fauxpas.ThrowingPredicate;
import org.zalando.riptide.Http;
import org.zalando.riptide.failsafe.CheckedPredicateConverter;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.micrometer.tag.RetryTagGenerator;
import org.zalando.riptide.micrometer.tag.StaticTagDecorator;

import javax.annotation.Nullable;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.fauxpas.FauxPas.throwingPredicate;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;

final class MicrometerPluginTest {

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final ClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
            HttpClientBuilder.create()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setSocketTimeout(500)
                            .build())
            .build());

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(driver.getBaseUrl())
            .plugin(new MicrometerPlugin(registry)
                    .withMetricName("http.outgoing-requests")
                    .withDefaultTags(Tag.of("client", "example"))
                    .withAdditionalTagGenerators(
                            new StaticTagDecorator(singleton(Tag.of("test", "true"))))
                    .withAdditionalTagGenerators(new RetryTagGenerator()))
            .plugin(new FailsafePlugin()
                .withPolicy(RetryPolicy.<ClientHttpResponse>builder()
                        .handleIf(error -> false)
                        .handleResultIf(toCheckedPredicate(throwingPredicate(response -> response.getStatusCode()
                                .is5xxServerError())))
                        .build()))
            .build();

    @Test
    void shouldRecordSuccessResponseMetric() {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().withStatus(200));

        unit.get("/foo")
                .call(pass())
                .join();

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("GET"));
        assertThat(timer.getId().getTag("http.path"), is("/foo"));
        assertThat(timer.getId().getTag("http.status_code"), is("200"));
        assertThat(timer.getId().getTag("peer.hostname"), is("localhost"));
        assertThat(timer.getId().getTag("error.kind"), is("none"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
    }

    @Test
    void shouldRecordRetryNumberMetricTag() {
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().withStatus(500));
        driver.addExpectation(onRequestTo("/foo"),
                giveEmptyResponse().withStatus(200));

        unit.get("/foo")
                .call(pass())
                .join();

        assertThat(search().timers(), iterableWithSize(2));

        {
            final Timer timer = search().tag("http.status_code", "500").timer();
            assertThat(timer, is(notNullValue()));
            assertThat(timer.getId().getTag("http.method"), is("GET"));
            assertThat(timer.getId().getTag("http.path"), is("/foo"));
            assertThat(timer.getId().getTag("http.status_code"), is("500"));
            assertThat(timer.getId().getTag("peer.hostname"), is("localhost"));
            assertThat(timer.getId().getTag("error.kind"), is("none"));
            assertThat(timer.getId().getTag("retry_number"), is("0"));
            assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
        }

        {
            final Timer timer = search().tag("http.status_code", "200").timer();
            assertThat(timer, is(notNullValue()));
            assertThat(timer.getId().getTag("http.method"), is("GET"));
            assertThat(timer.getId().getTag("http.path"), is("/foo"));
            assertThat(timer.getId().getTag("http.status_code"), is("200"));
            assertThat(timer.getId().getTag("peer.hostname"), is("localhost"));
            assertThat(timer.getId().getTag("error.kind"), is("none"));
            assertThat(timer.getId().getTag("retry_number"), is("1"));
            assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
        }
    }

    @Test
    void shouldRecordErrorResponseMetric() {
        driver.addExpectation(onRequestTo("/bar").withMethod(POST),
                giveEmptyResponse().withStatus(503));

        unit.post(URI.create("/bar"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .exceptionally(e -> null)
                .join();

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("POST"));
        assertThat(timer.getId().getTag("http.path"), is(""));
        assertThat(timer.getId().getTag("http.status_code"), is("503"));
        assertThat(timer.getId().getTag("peer.hostname"), is("localhost"));
        assertThat(timer.getId().getTag("error.kind"), is("none"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
    }

    @Test
    void shouldNotRecordFailureMetric() {
        driver.addExpectation(onRequestTo("/err"),
                giveEmptyResponse().after(750, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/err").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("GET"));
        assertThat(timer.getId().getTag("http.path"), is("/err"));
        assertThat(timer.getId().getTag("http.status_code"), is("0"));
        assertThat(timer.getId().getTag("peer.hostname"), is("localhost"));
        assertThat(timer.getId().getTag("error.kind"), is("SocketTimeoutException"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));
    }

    private Search search() {
        return registry.find("http.outgoing-requests");
    }

}
