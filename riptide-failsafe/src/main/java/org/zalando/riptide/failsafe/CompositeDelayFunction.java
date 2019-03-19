package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import net.jodah.failsafe.ExecutionContext;
import net.jodah.failsafe.RetryPolicy.DelayFunction;
import org.apiguardian.api.API;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class CompositeDelayFunction<R, X extends Throwable>  implements DelayFunction<R, X> {

    private final Collection<DelayFunction<R, X>> functions;

    @Override
    public Duration computeDelay(final R result, final X failure, final ExecutionContext context) {
        return functions.stream()
                .map(function -> function.computeDelay(result, failure, context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
