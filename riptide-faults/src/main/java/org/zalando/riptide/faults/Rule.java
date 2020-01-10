package org.zalando.riptide.faults;

import lombok.AllArgsConstructor;
import lombok.With;
import org.apiguardian.api.API;

import java.util.function.Predicate;

import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.faults.Predicates.alwaysFalse;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(staticName = "of")
public final class Rule<T> implements Predicate<T> {

    @With(PRIVATE)
    private final Predicate<T> include;

    @With(PRIVATE)
    private final Predicate<T> exclude;

    @Override
    public boolean test(final T input) {
        return include.test(input) && !exclude.test(input);
    }

    public static <T> Rule<T> of() {
        return of(alwaysTrue());
    }

    public static <T> Rule<T> of(final Predicate<T> predicate) {
        return of(predicate, alwaysFalse());
    }

    public Rule<T> include(final Predicate<T> predicate) {
        return withInclude(include.and(predicate));
    }

    public Rule<T> exclude(final Predicate<T> predicate) {
        return withExclude(exclude.or(predicate));
    }

}
