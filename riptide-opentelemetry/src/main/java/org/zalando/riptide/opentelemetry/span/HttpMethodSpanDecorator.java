package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.SemanticAttributes;
import org.zalando.riptide.RequestArguments;

public class HttpMethodSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(SemanticAttributes.HTTP_METHOD, arguments.getMethod().name());
    }
}
