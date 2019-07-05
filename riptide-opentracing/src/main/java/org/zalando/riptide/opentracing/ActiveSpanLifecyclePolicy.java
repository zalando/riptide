package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see Tracer#activeSpan()
 */
@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class ActiveSpanLifecyclePolicy implements LifecyclePolicy {

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        return Optional.ofNullable(tracer.activeSpan());
    }

    @Override
    public void finish(final Span span) {
        // nothing to do since we don't want to finish the active span
    }

}
