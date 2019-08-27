package org.zalando.riptide.opentracing;

import io.opentracing.*;
import lombok.*;
import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

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
