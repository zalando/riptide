package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import org.zalando.riptide.RequestArguments;

public class HttpHostSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(HttpIncubatingAttributes.HTTP_HOST, arguments.getRequestUri().getHost());
    }
}
