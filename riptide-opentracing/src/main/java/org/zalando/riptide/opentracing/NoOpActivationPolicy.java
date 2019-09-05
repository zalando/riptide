package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class NoOpActivationPolicy implements ActivationPolicy {

    @Override
    public Runnable activate(final Tracer tracer, final Span span) {
        return () -> {
            // nothing to do
        };
    }

}
