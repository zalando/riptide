package org.zalando.riptide.idempotency;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.zalando.riptide.RequestArguments;

import java.util.Set;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpMethod.*;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.1</a>
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.2">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.2</a>
 */
@API(status = EXPERIMENTAL)
public final class DefaultIdempotencyDetector implements IdempotencyDetector {
    private final Set<HttpMethod> ACCEPTED_METHODS = Set.of(DELETE, GET, HEAD, OPTIONS, PUT, TRACE);

    @Override
    public Decision test(final RequestArguments arguments, final Test root) {
        if (ACCEPTED_METHODS.contains(arguments.getMethod())) {
            return Decision.ACCEPT;
        }

        return Decision.NEUTRAL;
    }

}
