package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

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
