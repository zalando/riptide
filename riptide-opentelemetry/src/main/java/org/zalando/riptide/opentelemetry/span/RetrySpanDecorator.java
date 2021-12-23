package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.Attributes;
import org.zalando.riptide.RequestArguments;

import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.RETRY;
import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.RETRY_NUMBER;

public class RetrySpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        arguments.getAttribute(Attributes.RETRIES).ifPresent(retries -> {
            span.setAttribute(RETRY, true);
            span.setAttribute(RETRY_NUMBER, retries);
        });
    }
}
