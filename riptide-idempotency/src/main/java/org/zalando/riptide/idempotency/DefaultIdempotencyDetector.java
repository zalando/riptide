package org.zalando.riptide.idempotency;

import org.apiguardian.api.*;
import org.zalando.riptide.*;

import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.1</a>
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.2">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.2</a>
 */
@API(status = EXPERIMENTAL)
public final class DefaultIdempotencyDetector implements IdempotencyDetector {

    @Override
    public boolean test(final RequestArguments arguments, final Predicate<RequestArguments> root) {
        switch (arguments.getMethod()) {
            case DELETE:
            case GET:
            case HEAD:
            case OPTIONS:
            case PUT:
            case TRACE:
                return true;
            default:
                return false;
        }
    }

}
