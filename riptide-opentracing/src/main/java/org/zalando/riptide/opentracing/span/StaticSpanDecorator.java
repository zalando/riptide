package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;

import java.util.Map;

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
