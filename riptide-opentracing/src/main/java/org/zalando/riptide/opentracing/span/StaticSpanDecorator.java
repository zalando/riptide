package org.zalando.riptide.opentracing.span;

import io.opentracing.Tracer.SpanBuilder;
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
    public void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        tags.forEach(builder::withTag);
    }

}
