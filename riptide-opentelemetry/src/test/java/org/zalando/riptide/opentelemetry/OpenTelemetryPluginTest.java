package org.zalando.riptide.opentelemetry;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.opentelemetry.span.HttpHostSpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;
import org.zalando.riptide.opentelemetry.span.StaticSpanDecorator;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.GET;
import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;

class OpenTelemetryPluginTest {
    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private final Tracer tracer = otelTesting.getOpenTelemetry().getTracer("riptide-opentelemetry");

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    private final SpanDecorator environmentDecorator = new StaticSpanDecorator(singletonMap("env", "unittest"));

    private final Http unit = Http.builder()
                                  .executor(Executors.newCachedThreadPool())
                                  .requestFactory(new HttpComponentsClientHttpRequestFactory(
                                          HttpClientBuilder.create()
                                                           .setDefaultRequestConfig(
                                                                   RequestConfig.custom()
                                                                                .setSocketTimeout(500)
                                                                                .build())
                                                           .build()))
                                  .baseUrl(driver.getBaseUrl())
                                  .plugin(new OpenTelemetryPlugin(otelTesting.getOpenTelemetry(),
                                                                  environmentDecorator))
                                  .build();

    @Test
    void shouldTraceRequestAndResponse() {
        driver.addExpectation(onRequestTo("/users/me")
                                      .withMethod(POST)
                                      .withHeader("traceparent", notNullValue(String.class)),
                              giveResponse("Hello, world!", "text/plain")
                                      .withStatus(200)
                                      .withHeader("Retry-After", "60"));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            unit.post("/users/{user}", "me")
                .call(pass())
                .join();
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(2));

        assertThat(spans.get(1).getSpanId(), is(parent.getSpanContext().getSpanId()));

        final SpanData child = spans.get(0);
        assertThat(child.getParentSpanId(), is(parent.getSpanContext().getSpanId()));
        assertThat(child.getStatus(), is(not(StatusData.error())));

        final Attributes attributes = child.getAttributes();
        assertThat(attributes.get(AttributeKey.stringKey("env")), is("unittest"));
        assertThat(attributes.get(AttributeKey.stringKey("http.method")), is("POST"));
        assertThat(attributes.get(AttributeKey.longKey("http.status_code")), is(200L));
    }

    @Test
    void shouldTraceRequestAndServerError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(500));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            unit.get("/")
                .call(pass())
                .join();
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(2));

        assertThat(spans.get(1).getSpanId(), is(parent.getSpanContext().getSpanId()));

        final SpanData child = spans.get(0);
        assertThat(child.getParentSpanId(), is(parent.getSpanContext().getSpanId()));
        assertThat(child.getStatus(), is(StatusData.error()));

        final Attributes attributes = child.getAttributes();
        assertThat(attributes.get(AttributeKey.stringKey("env")), is("unittest"));
        assertThat(attributes.get(AttributeKey.stringKey("http.method")), is("GET"));
        assertThat(attributes.get(AttributeKey.longKey("http.status_code")), is(500L));
    }

    @Test
    void shouldTraceRequestAndNetworkError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().after(1, SECONDS));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            final CompletableFuture<?> future = unit.get(URI.create(driver.getBaseUrl()))
                                                    .call(noRoute());

            final CompletionException error = assertThrows(CompletionException.class, future::join);
            assertThat(error.getCause(), is(instanceOf(SocketTimeoutException.class)));
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(2));

        assertThat(spans.get(1).getSpanId(), is(parent.getSpanContext().getSpanId()));

        final SpanData child = spans.get(0);
        assertThat(child.getParentSpanId(), is(parent.getSpanContext().getSpanId()));

        final Attributes attributes = child.getAttributes();
        assertThat(attributes.get(AttributeKey.stringKey("env")), is("unittest"));
        assertThat(attributes.get(AttributeKey.stringKey("http.method")), is("GET"));
        assertThat(attributes.get(AttributeKey.longKey("http.status")), nullValue());

        assertThat(child.getStatus(), is(StatusData.error()));
        final List<EventData> events = child.getEvents();
        assertThat(events.size(), is(1));

        final Attributes eventAttributes = child.getEvents().get(0).getAttributes();
        assertThat(eventAttributes.get(SemanticAttributes.EXCEPTION_TYPE), containsString("CompletionException"));
        assertThat(eventAttributes.get(SemanticAttributes.EXCEPTION_MESSAGE), containsString("Read timed out"));
        assertThat(eventAttributes.get(SemanticAttributes.EXCEPTION_STACKTRACE), is(notNullValue()));
    }

    @Test
    void shouldTraceRequestAndIgnoreClientError() {
        driver.addExpectation(onRequestTo("/"), giveEmptyResponse().withStatus(400));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            unit.get("/")
                .call(pass())
                .join();
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(2));

        assertThat(spans.get(1).getSpanId(), is(parent.getSpanContext().getSpanId()));

        final SpanData child = spans.get(0);
        assertThat(child.getParentSpanId(), is(parent.getSpanContext().getSpanId()));
        assertThat(child.getStatus(), is(not(StatusData.error())));

        final Attributes attributes = child.getAttributes();
        assertThat(attributes.get(AttributeKey.stringKey("env")), is("unittest"));
        assertThat(attributes.get(AttributeKey.stringKey("http.method")), is("GET"));
        assertThat(attributes.get(AttributeKey.stringKey("peer.hostname")), is("localhost"));
        assertThat(attributes.get(AttributeKey.longKey("http.status_code")), is(400L));
    }

    @Test
    void shouldObtainTracerFromGlobalTelemetry() {
        OpenTelemetryPlugin plugin = new OpenTelemetryPlugin();
        assertThat(plugin.getTracer(), notNullValue());
    }

    @Test
    void shouldOverwriteDefaultSetOfDecorators() {
        final Http unit = Http.builder()
                              .executor(Executors.newCachedThreadPool())
                              .requestFactory(new HttpComponentsClientHttpRequestFactory(
                                      HttpClientBuilder.create()
                                                       .setDefaultRequestConfig(
                                                               RequestConfig.custom()
                                                                            .setSocketTimeout(500)
                                                                            .build())
                                                       .build()))
                              .baseUrl(driver.getBaseUrl())
                              .plugin(new OpenTelemetryPlugin(otelTesting.getOpenTelemetry())
                                              .withSpanDecorators(new HttpHostSpanDecorator()))
                              .build();

        driver.addExpectation(onRequestTo("/")
                                      .withMethod(GET)
                                      .withHeader("traceparent", notNullValue(String.class)),
                              giveResponse("Hello, world!", "text/plain")
                                      .withStatus(200));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            unit.get("/")
                .call(pass())
                .join();
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(2));

        assertThat(spans.get(1).getSpanId(), is(parent.getSpanContext().getSpanId()));

        final SpanData child = spans.get(0);
        assertThat(child.getParentSpanId(), is(parent.getSpanContext().getSpanId()));
        assertThat(child.getStatus(), is(not(StatusData.error())));

        final Attributes attributes = child.getAttributes();
        assertThat(attributes.size(), is(1));
        assertThat(attributes.get(AttributeKey.stringKey("http.host")), is("localhost"));
    }
}
