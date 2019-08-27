package org.zalando.riptide.opentracing.span;

import io.opentracing.*;
import org.zalando.riptide.*;

public final class TestSpanDecorator implements SpanDecorator {

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        span.setTag("spi", true);
    }

}
