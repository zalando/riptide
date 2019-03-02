package org.zalando.riptide.opentracing.span;

import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.zalando.riptide.RequestArguments;

/**
 * Sets the <code>component</code> span tag, defaults to <code>Riptide</code>.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class ComponentSpanDecorator implements SpanDecorator {

    private final String component;

    public ComponentSpanDecorator() {
        this("Riptide");
    }

    public ComponentSpanDecorator(final String component) {
        this.component = component;
    }

    @Override
    public void onStart(final Tracer.SpanBuilder builder, final RequestArguments arguments) {
        builder.withTag(Tags.COMPONENT, component);
    }

}
