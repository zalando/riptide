package org.zalando.riptide.micrometer;

import dev.failsafe.RetryPolicy;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.micrometer.tag.RetryTagGenerator;
import org.zalando.riptide.micrometer.tag.StaticTagDecorator;

import javax.annotation.Nullable;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.fauxpas.FauxPas.throwingPredicate;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;
import static org.zalando.riptide.micrometer.MockWebServerUtil.emptyMockResponse;
import static org.zalando.riptide.micrometer.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.micrometer.MockWebServerUtil.verify;

final class MicrometerPluginTest {

    private final MockWebServer server = new MockWebServer();

    private final ConnectionConfig connConfig = ConnectionConfig.custom()
            .setSocketTimeout(500, TimeUnit.MILLISECONDS)
            .build();

    private final BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();

    {
        cm.setConnectionConfig(connConfig);
    }

    private final ClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(
            HttpClientBuilder.create()
                    .setConnectionManager(cm)
                    .build());

    private final MeterRegistry registry = new SimpleMeterRegistry();

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(factory)
            .baseUrl(getBaseUrl(server))
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

    @AfterEach
    @SneakyThrows
    void shutdownServer() {
        server.shutdown();
    }

    @Test
    void shouldRecordSuccessResponseMetric() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        unit.get("/foo")
                .call(pass())
                .join();

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("GET"));
        assertThat(timer.getId().getTag("http.path"), is("/foo"));
        assertThat(timer.getId().getTag("http.status_code"), is("200"));
        assertThat(timer.getId().getTag("peer.hostname"), anyOf(is("hostname"), is("127.0.0.1")));
        assertThat(timer.getId().getTag("error.kind"), is("none"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));

        verify(server, 1, "/foo");
    }

    @Test
    void shouldRecordRetryNumberMetricTag() {
        server.enqueue(new MockResponse().setResponseCode(500));
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

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
            assertThat(timer.getId().getTag("peer.hostname"), anyOf(is("hostname"), is("127.0.0.1")));
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
            assertThat(timer.getId().getTag("peer.hostname"), anyOf(is("hostname"), is("127.0.0.1")));
            assertThat(timer.getId().getTag("error.kind"), is("none"));
            assertThat(timer.getId().getTag("retry_number"), is("1"));
            assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));
        }

        verify(server, 2, "/foo");
    }

    @Test
    void shouldRecordErrorResponseMetric() {
        server.enqueue(new MockResponse().setResponseCode(500));

        unit.post(URI.create("/bar"))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .exceptionally(e -> null)
                .join();

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("POST"));
        assertThat(timer.getId().getTag("http.path"), is(""));
        assertThat(timer.getId().getTag("http.status_code"), is("500"));
        assertThat(timer.getId().getTag("peer.hostname"), anyOf(is("hostname"), is("127.0.0.1")));
        assertThat(timer.getId().getTag("error.kind"), is("none"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));
        assertThat(timer.totalTime(NANOSECONDS), is(greaterThan(0.0)));

        verify(server, 1, "/bar", POST.toString());
    }

    @Test
    void shouldNotRecordFailureMetric() {
        server.enqueue(emptyMockResponse().setHeadersDelay(750, MILLISECONDS));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/err").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));

        @Nullable final Timer timer = search().timer();

        assertThat(timer, is(notNullValue()));
        assertThat(timer.getId().getTag("http.method"), is("GET"));
        assertThat(timer.getId().getTag("http.path"), is("/err"));
        assertThat(timer.getId().getTag("http.status_code"), is("0"));
        assertThat(timer.getId().getTag("peer.hostname"), anyOf(is("hostname"), is("127.0.0.1")));
        assertThat(timer.getId().getTag("error.kind"), is("SocketTimeoutException"));
        assertThat(timer.getId().getTag("client"), is("example"));
        assertThat(timer.getId().getTag("test"), is("true"));

        verify(server, 1, "/err");
    }

    private Search search() {
        return registry.find("http.outgoing-requests");
    }

}
