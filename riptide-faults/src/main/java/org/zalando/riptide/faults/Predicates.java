package org.zalando.riptide.faults;

import org.apiguardian.api.API;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class Predicates {

    private Predicates() {
        // nothing to do
    }

    @SafeVarargs
    public static <T> Predicate<T> or(
            final Predicate<T> predicate,
            final Predicate<T>... predicates) {
        return Stream.of(predicates).reduce(predicate, Predicate::or);
    }

    public static <T> Predicate<T> alwaysTrue() {
        return ignored -> true;
    }

    public static <T> Predicate<T> alwaysFalse() {
        return ignored -> false;
    }

}
