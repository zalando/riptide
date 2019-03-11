package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://stripe.com/docs/api#idempotent_requests">Stripe API: Idempotent Requests</a>
 */
@API(status = EXPERIMENTAL)
public final class IdempotencyKeyIdempotencyDetector implements IdempotencyDetector {

    @Override
    public boolean test(final RequestArguments arguments, final Predicate<RequestArguments> root) {
        return arguments.getHeaders().containsKey("Idempotency-Key");
    }

}
