package org.zalando.riptide.opentracing.span;

import com.google.common.collect.Lists;
import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

public interface SpanDecorator {

    default void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        // nothing to do
    }

    default void onStarted(final Span span, final RequestArguments arguments) {
        // nothing to do
    }

    default void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response)
            throws IOException {
        // nothing to do
    }

    default void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        // nothing to do
    }

    static SpanDecorator composite(final SpanDecorator decorator, final SpanDecorator... decorators) {
        return composite(Lists.asList(decorator, decorators));
    }

    static SpanDecorator composite(final Collection<SpanDecorator> decorators) {
        // we flatten first level of nested composite decorators
        return decorators.stream()
                .flatMap(decorator -> decorator instanceof CompositeSpanDecorator ?
                        CompositeSpanDecorator.class.cast(decorator).getDecorators().stream() :
                        Stream.of(decorator))
                .collect(collectingAndThen(toList(), CompositeSpanDecorator::new));
    }

}
