package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import org.zalando.riptide.RequestArguments;

public class HttpMethodSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(HttpIncubatingAttributes.HTTP_METHOD, arguments.getMethod().name());
    }
}
