package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;

import java.net.URI;

import static io.opentracing.tag.Tags.PEER_HOSTNAME;
import static io.opentracing.tag.Tags.PEER_PORT;
import static org.zalando.riptide.opentracing.ExtensionTags.PEER_ADDRESS;

/**
 * Sets the <code>peer.hostname</code> and <code>peer.port</code> span tags.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class PeerSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        final URI uri = arguments.getRequestUri();
        final int port = uri.getPort();

        span.setTag(PEER_HOSTNAME, uri.getHost());

        if (port == -1) {
            span.setTag(PEER_ADDRESS, uri.getHost());
        } else {
            span.setTag(PEER_ADDRESS, uri.getHost() + ":" + port);
            span.setTag(PEER_PORT, port);
        }
    }

}
