package org.zalando.riptide;

import org.apiguardian.api.API;

import javax.annotation.Nonnull;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://stripe.com/docs/api#idempotent_requests">Stripe API: Idempotent Requests</a>
 */
@API(status = EXPERIMENTAL)
public final class IdempotencyKeyIdempotentMethodDetector implements MethodDetector {

    @Override
    public boolean test(@Nonnull final RequestArguments arguments) {
        return arguments.getHeaders().keySet().stream().anyMatch("Idempotency-Key"::equalsIgnoreCase);
    }

}
