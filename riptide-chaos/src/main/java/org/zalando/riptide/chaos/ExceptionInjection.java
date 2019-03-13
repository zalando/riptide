package org.zalando.riptide.chaos;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestExecution;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

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
