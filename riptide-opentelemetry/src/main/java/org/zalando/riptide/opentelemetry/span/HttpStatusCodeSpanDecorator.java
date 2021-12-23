package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

public class HttpStatusCodeSpanDecorator implements SpanDecorator {

    @Override
    public void onResponse(Span span, RequestArguments arguments,
                           ClientHttpResponse response) throws IOException {
        span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, response.getRawStatusCode());
    }
}
