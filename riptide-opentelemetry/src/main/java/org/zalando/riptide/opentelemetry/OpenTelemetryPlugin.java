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

public class OpenTelemetryPlugin implements Plugin {

    public static final Attribute<String> OPERATION_NAME = Attribute.generate();

    private final Tracer tracer;
    private final TextMapPropagator propagator;

    public OpenTelemetryPlugin(final Tracer tracer) {
        this.tracer = tracer;
        this.propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public RequestExecution aroundAsync(@Nonnull final RequestExecution execution) {
        final Context context = Context.current();
        return arguments -> trace(context, execution, arguments);
    }

    private CompletableFuture<ClientHttpResponse> trace(
            final Context context,
            final RequestExecution execution,
            final RequestArguments arguments) throws IOException {

        final String operationName = arguments.getAttribute(OPERATION_NAME)
                                              .orElse(arguments.getMethod().name());
        final Span span = tracer.spanBuilder(operationName)
                                .setSpanKind(SpanKind.CLIENT)
                                .setParent(context)
                                .startSpan();

        final Map<String, String> headers = new HashMap<>();
        try (final Scope ignored = span.makeCurrent()) {
            this.propagator.inject(Context.current(), headers, Map::put);
        }

        return execution.execute(arguments.withHeaders(Multimaps.forMap(headers).asMap()))
                        .whenComplete((r, throwable) -> span.end());
    }
}
