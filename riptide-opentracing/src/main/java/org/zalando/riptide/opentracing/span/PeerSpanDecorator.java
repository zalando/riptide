package org.zalando.riptide.opentracing.span;

import io.opentracing.Tracer.SpanBuilder;
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
    public void onStart(final SpanBuilder builder, final RequestArguments arguments) {
        final URI requestUri = arguments.getRequestUri();
        builder.withTag(Tags.PEER_HOSTNAME, requestUri.getHost());
        builder.withTag(Tags.PEER_PORT, requestUri.getPort());
    }

}
