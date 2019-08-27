package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import io.opentracing.tag.*;
import org.zalando.riptide.*;

/**
 * Sets the <code>http.url</code> span tag.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class HttpUrlSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setTag(Tags.HTTP_URL, arguments.getRequestUri().toString());
    }

}
