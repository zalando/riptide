package org.zalando.riptide.opentracing.span;

import com.google.common.collect.*;
import io.opentracing.*;
import io.opentracing.log.*;
import io.opentracing.tag.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.io.*;

/**
 * Sets the <code>error</code> span tag as well as the <code>error.kind</code> and <code>error.object</code> span logs.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 * @see <a href="https://opentracing.io/specification/conventions/#log-fields-table">Standard Log Fields</a>
 */
public final class ErrorSpanDecorator implements SpanDecorator {

    @Override
    public void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().is5xxServerError()) {
            span.setTag(Tags.ERROR, true);
        }
    }

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        span.setTag(Tags.ERROR, true);
        span.log(ImmutableMap.of(
                Fields.ERROR_KIND, error.getClass().getSimpleName(),
                Fields.ERROR_OBJECT, error
        ));
    }

}
