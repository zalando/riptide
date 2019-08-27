package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import io.opentracing.log.*;
import org.zalando.riptide.*;

import static java.util.Collections.*;

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
