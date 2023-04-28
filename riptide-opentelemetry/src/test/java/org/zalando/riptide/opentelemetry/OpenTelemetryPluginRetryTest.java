package org.zalando.riptide.opentelemetry;

import dev.failsafe.RetryPolicy;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.opentelemetry.span.RetrySpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpStatus.OK;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.opentelemetry.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.opentelemetry.MockWebServerUtil.verify;

public class OpenTelemetryPluginRetryTest {
    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    private final Tracer tracer = otelTesting.getOpenTelemetry().getTracer("riptide-opentelemetry");

    private final MockWebServer server = new MockWebServer();

    private final SpanDecorator retryDecorator = new RetrySpanDecorator();

    private final Http unit = Http.builder()
                                  .executor(Executors.newCachedThreadPool())
                                  .requestFactory(new HttpComponentsClientHttpRequestFactory(
                                          HttpClientBuilder.create()
                                                           .setDefaultRequestConfig(
                                                                   RequestConfig.custom()
                                                                                .setSocketTimeout(500)
                                                                                .build())
                                                           .build()))
                                  .baseUrl(getBaseUrl(server))
                                  .plugin(new OpenTelemetryPlugin(otelTesting.getOpenTelemetry(), retryDecorator))
                                  .plugin(new FailsafePlugin()
                                                  .withPolicy(RetryPolicy.<ClientHttpResponse>builder()
                                                                      .withMaxRetries(2)
                                                                      .handleResultIf(response -> true)
                                                          .build()))
                                  .build();

    @Test
    void shouldTraceRetries() {
        server.enqueue(new MockResponse().setResponseCode(OK.value()));
        server.enqueue(new MockResponse().setResponseCode(OK.value()));
        server.enqueue(new MockResponse().setResponseCode(OK.value()));

        final Span parent = tracer.spanBuilder("test").startSpan();

        try (final Scope ignored = parent.makeCurrent()) {
            unit.get("/")
                .call(pass())
                .join();
        } finally {
            parent.end();
        }

        final List<SpanData> spans = otelTesting.getSpans();

        assertThat(spans, hasSize(4));

        final List<SpanData> retrySpans = spans.stream()
                                               .filter(this::hasRetryAttribute)
                                               .sorted(Comparator.comparing(SpanData::getStartEpochNanos))
                                               .collect(toList());

        assertThat(retrySpans, hasSize(2));

        long retryAttempt = 1;

        for (SpanData retrySpan : retrySpans) {
            Attributes attributes = retrySpan.getAttributes();
            assertThat(attributes.get(AttributeKey.longKey("retry_number")), is(retryAttempt++));
        }

        verify(server, 3, "/");
    }

    private boolean hasRetryAttribute(SpanData data) {
        return data.getAttributes().get(AttributeKey.booleanKey("retry")) != null;
    }
}
