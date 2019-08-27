package org.zalando.riptide.opentracing;

import com.google.common.annotations.*;
import com.google.common.collect.*;
import io.opentracing.*;
import io.opentracing.propagation.*;
import lombok.*;
import org.springframework.http.client.*;
import org.zalando.fauxpas.*;
import org.zalando.riptide.*;
import org.zalando.riptide.opentracing.span.*;

import javax.annotation.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import static com.google.common.collect.ImmutableList.*;
import static io.opentracing.propagation.Format.Builtin.*;
import static java.util.Objects.*;
import static java.util.ServiceLoader.*;
import static lombok.AccessLevel.*;

@AllArgsConstructor(access = PRIVATE)
public final class OpenTracingPlugin implements Plugin {

    /**
     * Allows to pass an explicit {@link Span} directly from a call site.
     */
    public static final Attribute<Span> SPAN = Attribute.generate();

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

    /**
     * Internal: Allows to pass span objects between stages.
     */
    private static final Attribute<Span> INTERNAL_SPAN = Attribute.generate();

    private final Tracer tracer;
    private final LifecyclePolicy lifecyclePolicy;
    private final ActivationPolicy activationPolicy;
    private final SpanDecorator decorator;

    public OpenTracingPlugin(final Tracer tracer) {
        this(tracer,
                LifecyclePolicy.composite(
                        new ExplicitSpanLifecyclePolicy(),
                        new NewSpanLifecyclePolicy()),
                new DefaultActivationPolicy(),
                SpanDecorator.composite(
                        new CallSiteSpanDecorator(),
                        new ComponentSpanDecorator(),
                        new ErrorSpanDecorator(),
                        new ErrorStackSpanDecorator(),
                        new HttpMethodSpanDecorator(),
                        new HttpPathSpanDecorator(),
                        new HttpStatusCodeSpanDecorator(),
                        new PeerSpanDecorator(),
                        new SpanKindSpanDecorator(),
                        SpanDecorator.composite(copyOf(load(SpanDecorator.class)))
                ));
    }

    @CheckReturnValue
    public OpenTracingPlugin withLifecyclePolicy(final LifecyclePolicy lifecyclePolicy) {
        return new OpenTracingPlugin(tracer, lifecyclePolicy, activationPolicy, decorator);
    }

    @CheckReturnValue
    public OpenTracingPlugin withActivationPolicy(final ActivationPolicy activationPolicy) {
        return new OpenTracingPlugin(tracer, lifecyclePolicy, activationPolicy, decorator);
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
        return new OpenTracingPlugin(tracer,
                lifecyclePolicy, activationPolicy, SpanDecorator.composite(decorator, decorators));
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> {
            @Nullable final Span span = lifecyclePolicy.start(tracer, arguments).orElse(null);

            if (span == null) {
                return execution.execute(arguments);
            }

            decorator.onRequest(span, arguments);

            final Runnable close = activationPolicy.activate(tracer, span);
            final Runnable finish = () -> lifecyclePolicy.finish(span);

            return execution.execute(arguments.withAttribute(INTERNAL_SPAN, span))
                    .whenComplete(perform(close, finish));
        };
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            @Nullable final Span span = arguments.getAttribute(INTERNAL_SPAN).orElse(null);

            if (span == null) {
                return execution.execute(arguments);
            }

            return execution.execute(inject(arguments, span.context()))
                    .whenComplete(decorate(span, arguments));
        };
    }

    private RequestArguments inject(final RequestArguments arguments, final SpanContext context) {
        final Map<String, String> headers = new HashMap<>();
        tracer.inject(context, HTTP_HEADERS, new TextMapAdapter(headers));
        return arguments.withHeaders(Multimaps.forMap(headers).asMap());
    }

    private ThrowingBiConsumer<ClientHttpResponse, Throwable, IOException> decorate(final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (nonNull(response)) {
                decorator.onResponse(span, arguments, response);
            }
            if (nonNull(error)) {
                decorator.onError(span, arguments, unpack(error));
            }
        };
    }

    private static <T, U> BiConsumer<T, U> perform(final Runnable... runnables) {
        return (t, u) -> Stream.of(runnables).forEach(Runnable::run);
    }

    @VisibleForTesting
    static Throwable unpack(final Throwable error) {
        return error instanceof CompletionException ? error.getCause() : error;
    }

}
