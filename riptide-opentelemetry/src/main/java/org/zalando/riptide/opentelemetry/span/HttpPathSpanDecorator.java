package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;

import static java.util.Objects.nonNull;

public class HttpPathSpanDecorator implements SpanDecorator {
    private static final AttributeKey<String> HTTP_PATH = AttributeKey.stringKey("http.path");

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        @Nullable
        final String uriTemplate = arguments.getUriTemplate();

        if (nonNull(uriTemplate)) {
            span.setAttribute(HTTP_PATH, uriTemplate);
        }
    }
}
