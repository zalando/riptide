package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

public class PeerHostSpanDecorator implements SpanDecorator {
    private static final AttributeKey<String> PEER_HOST = AttributeKey.stringKey("peer.hostname");

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setAttribute(PEER_HOST, arguments.getRequestUri().getHost());
    }
}
