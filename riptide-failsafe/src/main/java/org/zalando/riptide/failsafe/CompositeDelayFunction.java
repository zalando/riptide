package org.zalando.riptide.failsafe;

import lombok.*;
import net.jodah.failsafe.*;
import net.jodah.failsafe.function.*;
import org.apiguardian.api.*;

import java.time.*;
import java.util.*;

import static org.apiguardian.api.API.Status.*;

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
