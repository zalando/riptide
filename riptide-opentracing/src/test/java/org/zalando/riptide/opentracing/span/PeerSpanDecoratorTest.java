package org.zalando.riptide.opentracing.span;

import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.jupiter.api.Test;
import org.zalando.riptide.RequestArguments;

import java.net.URI;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

final class PeerSpanDecoratorTest {

    private final SpanDecorator unit = new PeerSpanDecorator();

    @Test
    void shouldIgnoreAbsentPort() {
        final MockTracer tracer = new MockTracer();
        final MockSpan span = tracer.buildSpan("test").start();

        final RequestArguments arguments = RequestArguments.create()
                .withBaseUrl(URI.create("http://localhost"))
                .withUriTemplate("/test");

        unit.onRequest(span, arguments);

        final Map<String, Object> tags = span.tags();

        assertThat(tags, hasEntry("peer.hostname", "localhost"));
        assertThat(tags, hasEntry("peer.address", "localhost"));
        assertThat(tags, not(hasKey("peer.port")));
    }

}
