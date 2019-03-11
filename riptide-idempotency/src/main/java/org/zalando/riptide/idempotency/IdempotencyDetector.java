package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.function.Predicate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface IdempotencyDetector {

    boolean test(final RequestArguments arguments, Predicate<RequestArguments> root);

}
