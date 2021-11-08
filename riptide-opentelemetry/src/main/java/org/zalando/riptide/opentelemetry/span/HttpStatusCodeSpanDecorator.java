package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

public class HttpStatusCodeSpanDecorator implements SpanDecorator {
    private static final AttributeKey<Long> HTTP_STATUS = AttributeKey.longKey("http.status");

    @Override
    public void onResponse(Span span, RequestArguments arguments,
                           ClientHttpResponse response) throws IOException {
        span.setAttribute(HTTP_STATUS, response.getRawStatusCode());
    }
}
