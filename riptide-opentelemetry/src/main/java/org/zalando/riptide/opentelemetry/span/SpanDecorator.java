package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

public interface SpanDecorator {
    default void onRequest(final Span span, final RequestArguments arguments) {
        // nothing to do
    }

    default void onResponse(final Span span, final RequestArguments arguments, final ClientHttpResponse response)
            throws IOException {
        // nothing to do
    }

    default void onError(final Span span, final RequestArguments arguments, final Throwable error) {
        // nothing to do
    }
}
