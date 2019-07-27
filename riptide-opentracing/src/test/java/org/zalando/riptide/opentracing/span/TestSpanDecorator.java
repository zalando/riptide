package org.zalando.riptide.opentracing.span;

import io.opentracing.Span;
import org.zalando.riptide.RequestArguments;

public final class TestSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setTag("spi", true);
    }

}
