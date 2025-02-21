package org.zalando.riptide.opentelemetry;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.ExceptionAttributes;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.*;
import org.zalando.riptide.opentelemetry.span.HttpHostSpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;
import org.zalando.riptide.opentelemetry.span.StaticSpanDecorator;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.zalando.riptide.NoRoute.noRoute;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentelemetry.MockWebServerUtil.*;

class OpenTelemetryPluginTest {
    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private final Tracer tracer = otelTesting.getOpenTelemetry().getTracer("riptide-opentelemetry");

    private final MockWebServer server = new MockWebServer();

    private final SpanDecorator environmentDecorator = new StaticSpanDecorator(singletonMap("env", "unittest"));

    private final ConnectionConfig connConfig = ConnectionConfig.custom()
            .setSocketTimeout(500, TimeUnit.MILLISECONDS)
            .build();

    private final BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();

    {
        cm.setConnectionConfig(connConfig);
    }

    private final Http unit = Http.builder()
            .executor(Executors.newCachedThreadPool())
            .requestFactory(new HttpComponentsClientHttpRequestFactory(
                    HttpClientBuilder.create()
                            .setConnectionManager(cm)
                            .build()))
            .baseUrl(getBaseUrl(server))
            .plugin(new OpenTelemetryPlugin(otelTesting.getOpenTelemetry(),
                    environmentDecorator))
            .build();

    @AfterEach
    @SneakyThrows
    void shutdownServer() {
        server.shutdown();
    }

    public static Stream<Arguments> routes() {
        Map<String, Function<AttributeStage, CompletableFuture<ClientHttpResponse>>> args = ImmutableMap.of(
                //this is an invalid config, because the route in incomplete, but we want to have the http status code nevertheless
                "incomplete route", stage -> stage.dispatch(Navigators.status(), Bindings.on(HttpStatus.OK)
                        .call(pass())),
                "complete route", stage -> stage.call(pass())
        );
        return args.entrySet().stream().map(a -> Arguments.of(a.getKey(), a.getValue()));
    }

    @Test
    void shouldTraceRequestAndResponse() {
        server.enqueue(textMockResponse("Hello, world!")
                .setHeader("Retry-After", "60"));

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

        verify(server, 1, "/users/me", POST.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("routes")
    void shouldTraceRequestAndServerError(@SuppressWarnings("unused") String name,
                                          Function<AttributeStage, CompletableFuture<ClientHttpResponse>> route) {
        server.enqueue(new MockResponse().setResponseCode(500));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            route.apply(unit.get("/")).join();
        } catch (Exception e) {
            //ignore
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

        verify(server, 1, "/");
    }

    @Test
    void shouldTraceRequestAndNetworkError() {
        server.enqueue(emptyMockResponse().setHeadersDelay(1, SECONDS));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            final CompletableFuture<?> future = unit.get(URI.create(getBaseUrl(server)))
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
        assertThat(eventAttributes.get(ExceptionAttributes.EXCEPTION_TYPE), containsString("SocketTimeoutException"));
        assertThat(eventAttributes.get(ExceptionAttributes.EXCEPTION_MESSAGE), containsString("Read timed out"));
        assertThat(eventAttributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE), is(notNullValue()));

        verify(server, 1, "/");
    }

    @Test
    void shouldTraceRequestAndIgnoreClientError() {
        server.enqueue(new MockResponse().setResponseCode(400));

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
        assertThat(attributes.get(AttributeKey.stringKey("peer.hostname")), anyOf(is("localhost"), is("127.0.0.1")));
        assertThat(attributes.get(AttributeKey.longKey("http.status_code")), is(400L));

        verify(server, 1, "/");
    }

    @Test
    void shouldObtainTracerFromGlobalTelemetry() {
        OpenTelemetryPlugin plugin = new OpenTelemetryPlugin();
        assertThat(plugin.getTracer(), notNullValue());
    }

    @Test
    void shouldActivateScope() throws IOException {
        // when running in java agent, the downstream http client is also instrumented
        // therefore it's important that the Scope is activated while we call the downstream client

        RequestExecution requestExecution = Mockito.mock(RequestExecution.class);
        String newName = "client span was activated";
        Mockito.when(requestExecution.execute(Mockito.any())).thenAnswer(invocationOnMock -> {
            Span.current().updateName(newName);
            return CompletableFuture.completedFuture(null);
        });
        new OpenTelemetryPlugin().aroundAsync(requestExecution)
                .execute(RequestArguments.create()
                        .withMethod(HttpMethod.GET)
                        .withUri(URI.create("https://example.com"))
                        .withAttribute(OpenTelemetryPlugin.OPERATION_NAME, "client"));
        otelTesting.assertTraces().singleElement().singleElement().hasName(newName);
    }

    @Test
    void shouldOverwriteDefaultSetOfDecorators() {
        final Http unit = Http.builder()
                .executor(Executors.newCachedThreadPool())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(
                        HttpClientBuilder.create()
                                .setConnectionManager(cm)
                                .build()))
                .baseUrl(getBaseUrl(server))
                .plugin(new OpenTelemetryPlugin(otelTesting.getOpenTelemetry())
                        .withSpanDecorators(new HttpHostSpanDecorator()))
                .build();

        server.enqueue(textMockResponse("Hello, world!"));

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
        assertThat(attributes.get(AttributeKey.stringKey("http.host")), anyOf(is("localhost"), is("127.0.0.1")));

        verify(server, 1, "/");
    }
}
