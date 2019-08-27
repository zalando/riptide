package org.zalando.riptide.opentracing.span;

import com.google.common.collect.*;
import io.opentracing.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

public interface SpanDecorator {

    default void onRequest(final Span span, final RequestArguments arguments) {
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
