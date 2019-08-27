package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import io.opentracing.tag.*;
import org.zalando.riptide.*;
import org.zalando.riptide.opentracing.*;

import java.net.*;

/**
 * Sets the <code>peer.hostname</code> and <code>peer.port</code> span tags.
 *
 * @see <a href="https://opentracing.io/specification/conventions/#span-tags-table">Standard Span Tags</a>
 */
public final class PeerSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        final URI requestUri = arguments.getRequestUri();
        span.setTag(ExtensionTags.PEER_ADDRESS, requestUri.getHost() + ":" + requestUri.getPort());
        span.setTag(Tags.PEER_HOSTNAME, requestUri.getHost());
        span.setTag(Tags.PEER_PORT, requestUri.getPort());
    }

}
