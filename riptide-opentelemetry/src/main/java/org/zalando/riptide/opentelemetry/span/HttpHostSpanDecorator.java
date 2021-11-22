package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

public class HttpHostSpanDecorator implements SpanDecorator {
    private static final AttributeKey<String> HTTP_HOST = AttributeKey.stringKey("http.host");

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(HTTP_HOST, arguments.getRequestUri().getHost());
    }
}
