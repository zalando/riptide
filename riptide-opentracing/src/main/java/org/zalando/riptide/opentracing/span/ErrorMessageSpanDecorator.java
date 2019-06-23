package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.log.Fields;
import org.zalando.riptide.RequestArguments;

import static java.util.Collections.singletonMap;

/**
 * Sets the <code>message</code> span log.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#log-fields-table">Standard Log Fields</a>
 */
public final class ErrorMessageSpanDecorator implements SpanDecorator {

    @Override
    public void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        span.log(singletonMap(Fields.MESSAGE, error.getMessage()));
    }

}
