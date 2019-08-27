package org.zalando.riptide.opentracing;

import io.opentracing.*;

public final class NoOpActivationPolicy implements ActivationPolicy {

    @Override
    public Runnable activate(final Tracer tracer, final Span span) {
        return () -> {
            // nothing to do
        };
    }

}
