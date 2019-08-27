package org.zalando.riptide.idempotency;

import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public interface IdempotencyDetector {

    boolean test(final RequestArguments arguments, Predicate<RequestArguments> root);

}
