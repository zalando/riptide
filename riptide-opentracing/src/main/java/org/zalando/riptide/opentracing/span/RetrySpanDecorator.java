package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import org.zalando.riptide.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.opentracing.*;

import static java.util.Collections.*;

/**
 * @see FailsafePlugin#ATTEMPTS
 * @see ExtensionTags#RETRY
 * @see ExtensionFields#RETRY_NUMBER
 */
public final class RetrySpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        arguments.getAttribute(FailsafePlugin.ATTEMPTS).ifPresent(retries -> {
            span.setTag(ExtensionTags.RETRY, true);
            span.log(singletonMap(ExtensionFields.RETRY_NUMBER, retries));
        });
    }

}
