package org.zalando.riptide.opentracing;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see Tracer#activateSpan(Span)
 * @see Scope#close()
 */
@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class DefaultActivationPolicy implements ActivationPolicy {

    @Override
    public Runnable activate(final Tracer tracer, final Span span) {
        return tracer.activateSpan(span)::close;
    }

}
