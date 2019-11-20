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
public final class ActiveSpanLifecycle implements Lifecycle {

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        return Optional.ofNullable(tracer.activeSpan())
                // we don't want to finish the active span
                .map(NonFinishingSpan::new);
    }

}
