package org.zalando.riptide.failsafe;

import dev.failsafe.ExecutionContext;
import dev.failsafe.function.ContextualSupplier;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class CompositeDelayFunction<R> implements ContextualSupplier<R, Duration> {

    private final Collection<ContextualSupplier<R, Duration>> functions;

    @Override
    public Duration get(final ExecutionContext<R> context) throws Throwable {
        return functions.stream()
                .map(function -> {
                    try {
                        return function.get(context);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @SafeVarargs
    public static <R, X extends Throwable> ContextualSupplier<R, Duration> composite(
            final ContextualSupplier<R, Duration>... functions) {
        return new CompositeDelayFunction<>(Arrays.asList(functions));
    }


}
