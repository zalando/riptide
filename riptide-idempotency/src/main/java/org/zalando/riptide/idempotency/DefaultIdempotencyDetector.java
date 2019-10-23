package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.1</a>
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.2">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.2</a>
 */
@API(status = EXPERIMENTAL)
public final class DefaultIdempotencyDetector implements IdempotencyDetector {

    @Override
    public Decision test(final RequestArguments arguments, final Test root) {
        switch (arguments.getMethod()) {
            case DELETE:
            case GET:
            case HEAD:
            case OPTIONS:
            case PUT:
            case TRACE:
                return Decision.ACCEPT;
            default:
                return Decision.NEUTRAL;
        }
    }

}
