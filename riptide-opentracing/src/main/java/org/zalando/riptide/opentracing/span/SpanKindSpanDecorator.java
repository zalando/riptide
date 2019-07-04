package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.zalando.riptide.RequestArguments;

/**
 * Sets the <code>span.kind</code> span tag.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class SpanKindSpanDecorator implements SpanDecorator {

    private final String kind;

    public SpanKindSpanDecorator() {
        this(Tags.SPAN_KIND_CLIENT);
    }

    public SpanKindSpanDecorator(final String kind) {
        this.kind = kind;
    }

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setTag(Tags.SPAN_KIND, kind);
    }

}
