package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import io.opentracing.tag.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;

/**
 * Sets the <code>http.status_code</code> span tag.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class HttpStatusCodeSpanDecorator implements SpanDecorator {

    @Override
    public void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response) throws IOException {
        span.setTag(Tags.HTTP_STATUS, response.getRawStatusCode());
    }
}
