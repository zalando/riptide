package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import io.opentracing.tag.Tags;
import org.zalando.riptide.RequestArguments;

import java.net.URI;

/**
 * Sets the <code>peer.hostname</code> and <code>peer.port</code> span tags.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class PeerSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        final URI requestUri = arguments.getRequestUri();
        span.setTag(Tags.PEER_HOSTNAME, requestUri.getHost());
        span.setTag(Tags.PEER_PORT, requestUri.getPort());
    }

}
