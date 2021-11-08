package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

public class HttpMethodSpanDecorator implements SpanDecorator {
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(HTTP_METHOD, arguments.getMethod().name());
    }
}
