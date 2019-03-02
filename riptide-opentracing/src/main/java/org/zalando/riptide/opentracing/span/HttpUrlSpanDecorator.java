package org.zalando.riptide.opentracing.span;

import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import org.zalando.riptide.RequestArguments;

/**
 * Sets the <code>http.url</code> span tag.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class HttpUrlSpanDecorator implements SpanDecorator {

    @Override
    public void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        builder.withTag(Tags.HTTP_URL, arguments.getRequestUri().toString());
    }

}
