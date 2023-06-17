package org.zalando.riptide.idempotency;

import com.google.common.collect.Ordering;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.idempotency.IdempotencyDetector.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.idempotency.Decision.DENY;
import static org.zalando.riptide.idempotency.Decision.NEUTRAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class IdempotencyPredicate implements Predicate<RequestArguments> {

    private final Collection<IdempotencyDetector> detectors;

    public IdempotencyPredicate() {
        this(Arrays.asList(
                new ConditionalIdempotencyDetector(),
                new DefaultIdempotencyDetector(),
                new ExplicitIdempotencyDetector(),
                new IdempotencyKeyIdempotencyDetector(),
                new MethodOverrideIdempotencyDetector()
        ));
    }

    @Override
    public boolean test(final RequestArguments arguments) {
        return decide(arguments) == Decision.ACCEPT;
    }

    private Decision decide(final RequestArguments arguments) {
        final Test test = modified ->
                arguments.equals(modified) ? NEUTRAL : decide(modified);

        Decision decision = NEUTRAL;

        for (final IdempotencyDetector detector : detectors) {
            final Decision current = detector.test(arguments, test);

            decision = max(decision, current);

            if (decision == DENY) {
                return decision;
            }
        }

        return decision;
    }

    private Decision max(final Decision left, final Decision right) {
        return Ordering.natural().max(left, right);
    }

}
