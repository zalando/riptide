package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import lombok.AllArgsConstructor;

@AllArgsConstructor
final class NonFinishingSpan extends ForwardingSpan {

    private final Span span;

    @Override
    protected Span delegate() {
        return span;
    }

    @Override
    public void finish() {
        // nothing to do
    }

    @Override
    public void finish(final long finishMicros) {
        // nothing to do
    }

}
