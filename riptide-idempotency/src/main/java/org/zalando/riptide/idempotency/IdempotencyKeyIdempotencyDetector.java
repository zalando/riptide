package org.zalando.riptide.idempotency;

import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

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
