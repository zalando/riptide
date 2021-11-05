package org.zalando.riptide.opentelemetry;

import com.google.common.collect.Multimaps;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OpenTelemetryPlugin implements Plugin {

    public static final Attribute<String> OPERATION_NAME = Attribute.generate();

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    private final Attribute<Span> internalSpan = Attribute.generate();

    public OpenTelemetryPlugin(final Tracer tracer) {
        this.tracer = tracer;
        this.propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public RequestExecution aroundAsync(@Nonnull final RequestExecution execution) {
        return arguments -> trace(execution, arguments);
    }

    @Override
    public RequestExecution aroundNetwork(@Nonnull final RequestExecution execution) {
        return arguments -> inject(execution, arguments);
    }

    private CompletableFuture<ClientHttpResponse> trace(
            final RequestExecution execution,
            final RequestArguments arguments) throws IOException {

        final String operationName = arguments.getAttribute(OPERATION_NAME)
                                              .orElse(arguments.getMethod().name());

        final Span span = tracer.spanBuilder(operationName)
                                .setSpanKind(SpanKind.CLIENT)
                                .startSpan();

        return execution.execute(arguments.withAttribute(internalSpan, span))
                        .whenComplete((response, throwable) -> span.end());
    }

    private CompletableFuture<ClientHttpResponse> inject(
            final RequestExecution execution,
            final RequestArguments arguments) throws IOException {

        @Nullable
        final Span span = arguments.getAttribute(internalSpan)
                                   .orElse(null);

        if (span == null) {
            return execution.execute(arguments);
        }

        try (final Scope ignored = span.makeCurrent()) {
            span.addEvent("Network call");
            Map<String, String> headers = new HashMap<>();
            this.propagator.inject(Context.current(), headers, Map::put);
            return execution.execute(arguments.withHeaders(Multimaps.forMap(headers).asMap()));
        }
    }
}
