package org.zalando.riptide.opentracing;

import io.opentracing.*;
import lombok.*;
import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.util.*;

import static org.apiguardian.api.API.Status.*;

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
