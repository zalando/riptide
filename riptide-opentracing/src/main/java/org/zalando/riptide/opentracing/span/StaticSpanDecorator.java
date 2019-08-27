package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import org.zalando.riptide.*;

import java.util.*;

/**
 * Sets arbitrary, static span tags.
 */
public final class StaticSpanDecorator implements SpanDecorator {

    private final Map<String, String> tags;

    public StaticSpanDecorator(final Map<String, String> tags) {
        this.tags = tags;
    }

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        tags.forEach(span::setTag);
    }

}
