package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.Attributes;
import org.zalando.riptide.RequestArguments;

public class RetrySpanDecorator implements SpanDecorator {
    private static final AttributeKey<Boolean> RETRY = AttributeKey.booleanKey("retry");
    private static final AttributeKey<Long> RETRY_NUMBER = AttributeKey.longKey("retry_number");

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        arguments.getAttribute(Attributes.RETRIES).ifPresent(retries -> {
            span.setAttribute(RETRY, true);
            span.setAttribute(RETRY_NUMBER, retries);
        });
    }
}
