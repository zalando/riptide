package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.idempotency.IdempotencyPredicate.IDEMPOTENT;

/**
 * @see IdempotencyPredicate#IDEMPOTENT
 */
@API(status = EXPERIMENTAL)
public final class ExplicitIdempotencyDetector implements IdempotencyDetector {

    @Override
    public Decision test(
            final RequestArguments arguments,
            final Test root) {
        return arguments.getAttribute(IDEMPOTENT)
                .map(this::translate)
                .orElse(Decision.NEUTRAL);
    }

    private Decision translate(final boolean idempotent) {
        return idempotent ? Decision.ACCEPT : Decision.DENY;
    }

}
