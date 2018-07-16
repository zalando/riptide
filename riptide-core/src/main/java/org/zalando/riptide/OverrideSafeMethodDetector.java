package org.zalando.riptide;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apiguardian.api.API;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpMethod;

import javax.annotation.Nonnull;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpMethod.POST;

/**
 * @see <a href="https://opensocial.github.io/spec/2.5.1/Core-API-Server.xml#rfc.section.2.1.1.1">OpenSocial Core API Server Specification 2.5.1, Section 2.1.1.1</a>
 */
@API(status = EXPERIMENTAL)
public final class OverrideSafeMethodDetector implements MethodDetector {

    // TODO decide whether to open this up, effectively allowing other detectors to inspect the override
    private final MethodDetector detector = new DefaultSafeMethodDetector();

    @Override
    public boolean test(@Nonnull final RequestArguments arguments) {
        if (arguments.getMethod() != POST) {
            return false;
        }

        @Nullable final String override = getOverride(arguments);

        if (override == null) {
            return false;
        }

        final HttpMethod method;

        try {
            method = HttpMethod.valueOf(override);
        } catch (final IllegalArgumentException e) {
            return false;
        }

        return detector.test(arguments.withMethod(method));
    }

    @Nullable
    private String getOverride(final RequestArguments arguments) {
        final Multimap<String, String> headers = arguments.getHeaders();
        // TODO should use case-insensitive comparison
        final Collection<String> overrides = headers.get("X-HTTP-Method-Override");
        return Iterables.getFirst(overrides, null);
    }

}
