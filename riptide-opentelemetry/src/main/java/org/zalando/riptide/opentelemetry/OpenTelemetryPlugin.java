package org.zalando.riptide.opentelemetry;

import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimaps;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.micrometer.v1_5.OpenTelemetryMeterRegistry;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingBiConsumer;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.opentelemetry.span.CompositeSpanDecorator;
import org.zalando.riptide.opentelemetry.span.ErrorSpanDecorator;
import org.zalando.riptide.opentelemetry.span.HttpMethodSpanDecorator;
import org.zalando.riptide.opentelemetry.span.HttpPathSpanDecorator;
import org.zalando.riptide.opentelemetry.span.HttpStatusCodeSpanDecorator;
import org.zalando.riptide.opentelemetry.span.PeerHostSpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

public class OpenTelemetryPlugin implements Plugin {

    public static final Attribute<String> OPERATION_NAME = Attribute.generate();

    private final OpenTelemetry telemetry;
    private final Tracer tracer;
    private final TextMapPropagator propagator;
    private final SpanDecorator spanDecorator;

    public OpenTelemetryPlugin(final SpanDecorator... decorators) {
        this(GlobalOpenTelemetry.get(), decorators);
    }

    public OpenTelemetryPlugin(final OpenTelemetry telemetry, final SpanDecorator... decorators) {
        this(telemetry, CompositeSpanDecorator.composite(
                new HttpMethodSpanDecorator(),
                new HttpStatusCodeSpanDecorator(),
                new ErrorSpanDecorator(),
                new PeerHostSpanDecorator(),
                new HttpPathSpanDecorator(),
                CompositeSpanDecorator.composite(decorators)
        ));
    }

    private OpenTelemetryPlugin(final OpenTelemetry telemetry, final SpanDecorator spanDecorator) {
        this.telemetry = telemetry;
        this.tracer = telemetry.getTracer("riptide-opentelemetry");
        this.propagator = telemetry.getPropagators().getTextMapPropagator();
        this.spanDecorator = spanDecorator;

        // todo should OpenTelemetryPlugin extend MicrometerPlugin
        new MicrometerPlugin(OpenTelemetryMeterRegistry.create(telemetry));
    }

    public OpenTelemetryPlugin withSpanDecorators(final SpanDecorator... decorators) {
        return new OpenTelemetryPlugin(telemetry, CompositeSpanDecorator.composite(decorators));
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

        spanDecorator.onRequest(span, arguments);

        final Map<String, String> headers = new HashMap<>();
        try (final Scope ignored = context.with(span).makeCurrent()) {
            this.propagator.inject(Context.current(), headers, Map::put);
        }

        return execution.execute(arguments.withHeaders(Multimaps.forMap(headers).asMap()))
                        .whenComplete(decorateOnResponse(span, arguments))
                        .whenComplete(decorateOnError(span, arguments))
                        .whenComplete((r, t) -> span.end());
    }

    private ThrowingBiConsumer<ClientHttpResponse, Throwable, IOException> decorateOnResponse(
            final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (response != null) {
                spanDecorator.onResponse(span, arguments, response);
            }
        };
    }

    private BiConsumer<ClientHttpResponse, Throwable> decorateOnError(
            final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (error != null) {
                spanDecorator.onError(span, arguments, unpack(error));
            }
        };
    }

    @VisibleForTesting
    static Throwable unpack(final Throwable error) {
        return error instanceof CompletionException ? error.getCause() : error;
    }

    @VisibleForTesting
    Tracer getTracer() {
        return tracer;
    }
}
