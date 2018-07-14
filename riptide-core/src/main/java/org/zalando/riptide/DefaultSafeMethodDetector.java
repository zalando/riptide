package org.zalando.riptide;

import org.apiguardian.api.API;

import javax.annotation.Nonnull;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">Hypertext Transfer Protocol (HTTP/1.1): Semantics and Content, Section 4.2.1</a>
 */
@API(status = EXPERIMENTAL)
public final class DefaultSafeMethodDetector implements MethodDetector {

    @Override
    public boolean test(@Nonnull final RequestArguments arguments) {
        switch (arguments.getMethod()) {
            case GET:
            case HEAD:
            case OPTIONS:
            case TRACE:
                return true;
            default:
                return false;
        }
    }

}
