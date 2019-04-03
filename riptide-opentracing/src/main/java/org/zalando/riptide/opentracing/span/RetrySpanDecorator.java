package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.opentracing.ExtensionFields;
import org.zalando.riptide.opentracing.ExtensionTags;

import static java.util.Collections.singletonMap;

/**
 * @see FailsafePlugin#ATTEMPTS
 * @see ExtensionTags#RETRY
 * @see ExtensionFields#RETRY_NUMBER
 */
public final class RetrySpanDecorator implements SpanDecorator {

    @Override
    public void onStarted(final Span span, final RequestArguments arguments) {
        arguments.getAttribute(FailsafePlugin.ATTEMPTS).ifPresent(retries -> {
            span.setTag(ExtensionTags.RETRY, true);
            span.log(singletonMap(ExtensionFields.RETRY_NUMBER, retries));
        });
    }

}
