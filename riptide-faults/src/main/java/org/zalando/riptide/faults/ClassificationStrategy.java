package org.zalando.riptide.faults;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Throwables.getCausalChain;
import static com.google.common.base.Throwables.getRootCause;

public interface ClassificationStrategy {

    boolean test(Throwable throwable, Predicate<Throwable> predicate);

    static ClassificationStrategy self() {
        return (throwable, predicate) ->
                predicate.test(throwable);
    }

    static ClassificationStrategy causalChain() {
        return (throwable, predicate) -> {
            @SuppressWarnings("UnstableApiUsage")
            final List<Throwable> chain = getCausalChain(throwable);
            return chain.stream().anyMatch(predicate);
        };
    }

    static ClassificationStrategy rootCause() {
        return (throwable, predicate) ->
                predicate.test(getRootCause(throwable));
    }

}
