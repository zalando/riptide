package org.zalando.riptide.idempotency;

import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.RequestArguments;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class IdempotencyPredicate implements Predicate<RequestArguments> {

    /**
     * Allows to override idempotency detection from a call site by specifying the attribute. This might be useful in
     * situation where the caller knows that a certain operation is idempotent, but it's undetectable using other means.
     *
     * @see <a href="https://nakadi.io/manual.html#/subscriptions/subscription_id/cursors_post">Nakadi API: Endpoint for committing offsets</a>
     */
    public static final Attribute<Boolean> IDEMPOTENT = Attribute.generate();

    private final Collection<IdempotencyDetector> detectors;

    public IdempotencyPredicate() {
        this(Arrays.asList(
                new DefaultIdempotencyDetector(),
                new ConditionalIdempotencyDetector(),
                new IdempotencyKeyIdempotencyDetector(),
                new MethodOverrideIdempotencyDetector()
        ));
    }

    @Override
    public boolean test(final RequestArguments arguments) {
        return arguments.getAttribute(IDEMPOTENT).orElseGet(() ->
            detectors.stream()
                .anyMatch(detector -> detector.test(arguments, modified ->
                        !arguments.equals(modified) && test(modified)))
        );
    }

}
