package org.zalando.riptide;

import org.apiguardian.api.API;

import javax.annotation.Nonnull;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.2">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.2</a>
 */
@API(status = EXPERIMENTAL)
public final class DefaultIdempotentMethodDetector implements MethodDetector {

    private final MethodDetector safe;

    public DefaultIdempotentMethodDetector() {
        this(new DefaultSafeMethodDetector());
    }

    public DefaultIdempotentMethodDetector(final MethodDetector safe) {
        this.safe = safe;
    }

    @Override
    public boolean test(@Nonnull final RequestArguments arguments) {
        if (safe.test(arguments)) {
            return true;
        }

        switch (arguments.getMethod()) {
            case PUT:
            case DELETE:
                return true;
            default:
                return false;
        }
    }

}
