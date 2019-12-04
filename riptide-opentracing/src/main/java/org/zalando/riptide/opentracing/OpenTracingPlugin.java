package org.zalando.riptide.opentracing;

import com.google.common.annotations.VisibleForTesting;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingBiConsumer;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.AttributeStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.opentracing.span.CallSiteSpanDecorator;
import org.zalando.riptide.opentracing.span.ComponentSpanDecorator;
import org.zalando.riptide.opentracing.span.CompositeSpanDecorator;
import org.zalando.riptide.opentracing.span.ErrorSpanDecorator;
import org.zalando.riptide.opentracing.span.ErrorStackSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpMethodOverrideSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpMethodSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpPathSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpPreferSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpRetryAfterSpanDecorator;
import org.zalando.riptide.opentracing.span.HttpStatusCodeSpanDecorator;
import org.zalando.riptide.opentracing.span.PeerSpanDecorator;
import org.zalando.riptide.opentracing.span.ServiceLoaderSpanDecorator;
import org.zalando.riptide.opentracing.span.SpanDecorator;
import org.zalando.riptide.opentracing.span.SpanKindSpanDecorator;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
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
    private final Attribute<Span> internalSpan = Attribute.generate();

    private final Tracer tracer;
    private final Lifecycle lifecycle;
    private final Activation activation;
    private final Injection injection;
    private final SpanDecorator decorator;

    public OpenTracingPlugin(final Tracer tracer) {
        this(tracer,
                Lifecycle.composite(
                        new ExplicitSpanLifecycle(),
                        new NewSpanLifecycle()),
                new DefaultActivation(),
                new DefaultInjection(),
                CompositeSpanDecorator.composite(
                        new CallSiteSpanDecorator(),
                        new ComponentSpanDecorator(),
                        new ErrorSpanDecorator(),
                        new ErrorStackSpanDecorator(),
                        new HttpMethodOverrideSpanDecorator(),
                        new HttpMethodSpanDecorator(),
                        new HttpPathSpanDecorator(),
                        new HttpPreferSpanDecorator(),
                        new HttpRetryAfterSpanDecorator(),
                        new HttpStatusCodeSpanDecorator(),
                        new PeerSpanDecorator(),
                        new ServiceLoaderSpanDecorator(),
                        new SpanKindSpanDecorator()
                ));
    }

    @CheckReturnValue
    public OpenTracingPlugin withLifecycle(final Lifecycle lifecycle) {
        return new OpenTracingPlugin(tracer, lifecycle, activation, injection, decorator);
    }

    @CheckReturnValue
    public OpenTracingPlugin withActivation(final Activation activation) {
        return new OpenTracingPlugin(tracer, lifecycle, activation, injection, decorator);
    }

    @CheckReturnValue
    public OpenTracingPlugin withInjection(final Injection injection) {
        return new OpenTracingPlugin(tracer, lifecycle, activation, injection, decorator);
    }

    /**
     * Creates a new {@link OpenTracingPlugin plugin} by
     * <strong>combining</strong> the {@link SpanDecorator decorator(s)} of
     * {@code this} plugin with the supplied ones.
     *
     * @param first      first decorator
     * @param decorators optional, remaining decorators
     * @return a new {@link OpenTracingPlugin}
     */
    @CheckReturnValue
    public OpenTracingPlugin withAdditionalSpanDecorators(final SpanDecorator first,
            final SpanDecorator... decorators) {
        return withSpanDecorators(decorator,
                CompositeSpanDecorator.composite(first, decorators));
    }

    /**
     * Creates a new {@link OpenTracingPlugin plugin} by
     * <strong>replacing</strong> the {@link SpanDecorator decorator(s)} of
     * {@code this} plugin with the supplied ones.
     *
     * @param decorator  first decorator
     * @param decorators optional, remaining decorators
     * @return a new {@link OpenTracingPlugin}
     */
    @CheckReturnValue
    public OpenTracingPlugin withSpanDecorators(
            final SpanDecorator decorator, final SpanDecorator... decorators) {
        return new OpenTracingPlugin(tracer,
                lifecycle, activation, injection,
                CompositeSpanDecorator.composite(decorator, decorators));
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return arguments -> trace(execution, arguments);
    }

    private CompletableFuture<ClientHttpResponse> trace(
            final RequestExecution execution,
            final RequestArguments arguments) throws IOException {

        @Nullable final Span span = lifecycle.start(tracer, arguments)
                .orElse(null);

        if (span == null) {
            return execution.execute(arguments);
        }

        final Scope scope = activation.activate(tracer, span);
        return execution.execute(arguments.withAttribute(internalSpan, span))
                .whenComplete(run(scope::close))
                .whenComplete(run(span::finish));
    }

    private <T, U> BiConsumer<T, U> run(final Runnable runnable) {
        return (t, U) -> runnable.run();
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> inject(execution, arguments);
    }

    private CompletableFuture<ClientHttpResponse> inject(
            final RequestExecution execution,
            final RequestArguments arguments) throws IOException {

        @Nullable final Span span = arguments.getAttribute(internalSpan)
                .orElse(null);

        if (span == null) {
            return execution.execute(arguments);
        }

        decorator.onRequest(span, arguments);

        final SpanContext context = span.context();
        return execution.execute(injection.inject(tracer, arguments, context))
                .whenComplete(decorateOnResponse(span, arguments))
                .whenComplete(decorateOnError(span, arguments));
    }

    private ThrowingBiConsumer<ClientHttpResponse, Throwable, IOException> decorateOnResponse(
            final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (nonNull(response)) {
                decorator.onResponse(span, arguments, response);
            }
        };
    }

    private BiConsumer<ClientHttpResponse, Throwable> decorateOnError(
            final Span span,
            final RequestArguments arguments) {

        return (response, error) -> {
            if (nonNull(error)) {
                decorator.onError(span, arguments, unpack(error));
            }
        };
    }

    @VisibleForTesting
    static Throwable unpack(final Throwable error) {
        return error instanceof CompletionException ? error.getCause() : error;
    }

}
