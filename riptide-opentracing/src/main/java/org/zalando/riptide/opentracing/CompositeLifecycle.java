package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.zalando.riptide.RequestArguments;

import java.util.Collection;
import java.util.Optional;

@AllArgsConstructor
final class CompositeLifecycle implements Lifecycle {

    private final Collection<Lifecycle> policies;

    @Override
    public Optional<Span> start(final Tracer tracer, final RequestArguments arguments) {
        return policies.stream()
                .map(policy -> policy.start(tracer, arguments))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

}
