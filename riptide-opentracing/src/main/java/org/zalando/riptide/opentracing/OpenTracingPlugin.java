package org.zalando.riptide.opentracing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimaps;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.TextMapAdapter;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingBiConsumer;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.AttributeStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.opentracing.span.CallSiteSpanDecorator;
import org.zalando.riptide.opentracing.span.ComponentSpanDecorator;
import org.zalando.riptide.opentracing.span.ErrorSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpMethodSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpPathSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpStatusCodeSpanDecorator;
import org.zalando.riptide.opentracing.span.PeerSpanDecorator;
import org.zalando.riptide.opentracing.span.SpanDecorator;
import org.zalando.riptide.opentracing.span.SpanKindSpanDecorator;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

import static io.opentracing.propagation.Format.Builtin.HTTP_HEADERS;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;

@AllArgsConstructor(access = PRIVATE)
public final class OpenTracingPlugin implements Plugin {

    /**
     * Allows to pass a customized {@link Tracer#buildSpan(String) operation name} directly from
     * a call site. Defaults to the {@link RequestArguments#getMethod() HTTP method}.
     *
     * @see AttributeStage#attribute(Attribute, Object)
     */
    public static final Attribute<String> OPERATION_NAME = Attribute.generate();

    /**
     * Allows to pass arbitrary span tags directly from a call site.
     *
     * @see AttributeStage#attribute(Attribute, Object)
     */
    public static final Attribute<Map<String, String>> TAGS = Attribute.generate();

    /**
     * Allows to pass arbitrary span logs directly from a call site.
     *
     * @see AttributeStage#attribute(Attribute, Object)
     */
    public static final Attribute<Map<String, Object>> LOGS = Attribute.generate();

    private final Tracer tracer;
    private final SpanDecorator decorator;

    public OpenTracingPlugin(final Tracer tracer) {
        this(tracer, SpanDecorator.composite(
                new CallSiteSpanDecorator(),
                new ComponentSpanDecorator(),
                new ErrorSpanDecorator(),
                new HttpMethodSpanDecorator(),
                new HttpPathSpanDecorator(),
                new HttpStatusCodeSpanDecorator(),
                new PeerSpanDecorator(),
                new SpanKindSpanDecorator()
        ));
    }

    /**
     * Creates a new {@link OpenTracingPlugin plugin} by <strong>combining</strong> the {@link SpanDecorator decorator(s)} of
     * {@code this} plugin with the supplied ones.
     *
     * @param first      first decorator
     * @param decorators optional, remaining decorators
     * @return a new {@link OpenTracingPlugin}
     */
    @CheckReturnValue
    public OpenTracingPlugin withAdditionalSpanDecorators(final SpanDecorator first,
            final SpanDecorator... decorators) {
        return withSpanDecorators(decorator, SpanDecorator.composite(first, decorators));
    }

    /**
     * Creates a new {@link OpenTracingPlugin plugin} by <strong>replacing</strong> the {@link SpanDecorator decorator(s)} of
     * {@code this} plugin with the supplied ones.
     *
     * @param decorator  first decorator
     * @param decorators optional, remaining decorators
     * @return a new {@link OpenTracingPlugin}
     */
    @CheckReturnValue
    public OpenTracingPlugin withSpanDecorators(final SpanDecorator decorator, final SpanDecorator... decorators) {
        return new OpenTracingPlugin(tracer, SpanDecorator.composite(decorator, decorators));
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> {
            final Span span = startSpan(arguments);
            final Scope scope = tracer.activateSpan(span);

            return execution.execute(arguments)
                    .whenComplete(perform(scope::close))
                    .whenComplete(perform(span::finish));
        };
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            final Span span = tracer.activeSpan();

            return execution.execute(inject(arguments, span.context()))
                    .whenComplete(onResponse(span, arguments))
                    .whenComplete(onError(span, arguments));
        };
    }

    private Span startSpan(final RequestArguments arguments) {
        final String operationName = arguments.getAttribute(OPERATION_NAME)
                .orElse(arguments.getMethod().name());

        final SpanBuilder builder = tracer.buildSpan(operationName);
        decorator.onStart(builder, arguments);
        final Span span = builder.start();
        decorator.onStarted(span, arguments);
        return span;
    }

    private RequestArguments inject(final RequestArguments arguments, final SpanContext context) {
        final Map<String, String> headers = new HashMap<>();
        tracer.inject(context, HTTP_HEADERS, new TextMapAdapter(headers));
        return arguments.withHeaders(Multimaps.forMap(headers).asMap());
    }

    private ThrowingBiConsumer<ClientHttpResponse, Throwable, IOException> onResponse(final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (nonNull(response)) {
                decorator.onResponse(span, arguments, response);
            }
        };
    }

    private BiConsumer<ClientHttpResponse, Throwable> onError(final Span span, final RequestArguments arguments) {
        return (response, error) -> {
            if (nonNull(error)) {
                decorator.onError(span, arguments, unpack(error));
            }
        };
    }

    private static <T, U> BiConsumer<T, U> perform(final Runnable runnable) {
        return (t, u) -> runnable.run();
    }

    @VisibleForTesting
    static Throwable unpack(final Throwable error) {
        return error instanceof CompletionException ? error.getCause() : error;
    }

}
