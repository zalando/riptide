package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.Attributes;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentracing.ExtensionFields;
import org.zalando.riptide.opentracing.ExtensionTags;

import static java.util.Collections.singletonMap;

/**
 * @see Attributes#RETRIES
 * @see ExtensionTags#RETRY
 * @see ExtensionFields#RETRY_NUMBER
 */
public final class RetrySpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        arguments.getAttribute(Attributes.RETRIES).ifPresent(retries -> {
            span.setTag(ExtensionTags.RETRY, true);
            span.log(singletonMap(ExtensionFields.RETRY_NUMBER, retries));
        });
    }

}
