package org.zalando.riptide;

import com.google.common.collect.Iterables;
import org.apiguardian.api.API;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpMethod;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.http.HttpMethod.POST;

/**
 * @see <a href="https://opensocial.github.io/spec/2.5.1/Core-API-Server.xml#rfc.section.2.1.1.1">OpenSocial Core API Server Specification 2.5.1, Section 2.1.1.1</a>
 */
@API(status = EXPERIMENTAL)
public final class OverrideSafeMethodDetector implements MethodDetector {

    private final MethodDetector detector = new DefaultSafeMethodDetector();

    @Override
    public boolean test(final RequestArguments arguments) {
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
        final Map<String, List<String>> headers = arguments.getHeaders();
        final Collection<String> overrides = headers.getOrDefault("X-HTTP-Method-Override", emptyList());
        return Iterables.getFirst(overrides, null);
    }

}
