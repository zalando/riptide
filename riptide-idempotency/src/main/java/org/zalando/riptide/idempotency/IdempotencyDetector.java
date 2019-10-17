package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface IdempotencyDetector {

    Decision test(final RequestArguments arguments, Test root);

    @FunctionalInterface
    interface Test {
        Decision test(RequestArguments arguments);
    }

}
