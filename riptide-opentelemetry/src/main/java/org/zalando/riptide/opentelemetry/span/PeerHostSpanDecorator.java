package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.PEER_HOST;

public class PeerHostSpanDecorator implements SpanDecorator {
    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(PEER_HOST, arguments.getRequestUri().getHost());
    }
}
