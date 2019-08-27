package org.zalando.riptide.chaos;

import lombok.*;
import lombok.extern.slf4j.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
@Slf4j
public final class ExceptionInjection implements FailureInjection {

    private final Probability probability;
    private final List<Supplier<? extends Exception>> exceptions;

    @Override
    public RequestExecution inject(final RequestExecution execution) {
        if (probability.test()) {
            return arguments -> {
                final CompletableFuture<ClientHttpResponse> future = execution.execute(arguments);
                return future.whenComplete((response, failure) -> {
                    if (failure == null) {
                        // only inject exception if not failed already
                        final Exception exception = choose().get();
                        log.debug("Injecting '{}'", exception.getClass().getSimpleName());
                        throw new CompletionException(exception);
                    }
                });
            };
        }

        return execution;
    }

    private Supplier<? extends Exception> choose() {
        return exceptions.get(ThreadLocalRandom.current().nextInt(exceptions.size()));
    }

}
