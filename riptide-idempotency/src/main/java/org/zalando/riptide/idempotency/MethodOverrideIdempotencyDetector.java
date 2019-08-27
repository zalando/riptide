package org.zalando.riptide.idempotency;

import lombok.extern.slf4j.*;
import org.apiguardian.api.*;
import org.springframework.http.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.util.*;
import java.util.function.*;

import static java.util.Collections.*;
import static org.apiguardian.api.API.Status.*;
import static org.springframework.http.HttpMethod.*;

/**
 * @see <a href="https://opensocial.github.io/spec/2.5.1/Core-API-Server.xml#rfc.section.2.1.1.1">OpenSocial Core API Server Specification 2.5.1, Section 2.1.1.1</a>
 */
@API(status = EXPERIMENTAL)
@Slf4j
public final class MethodOverrideIdempotencyDetector implements IdempotencyDetector {

    @Override
    public boolean test(final RequestArguments arguments, final Predicate<RequestArguments> root) {
        if (arguments.getMethod() != POST) {
            return false;
        }

        @Nullable final HttpMethod method = getOverride(arguments);

        if (method == null) {
            return false;
        }

        return root.test(arguments.withMethod(method));
    }

    @Nullable
    private HttpMethod getOverride(final RequestArguments arguments) {
        final Map<String, List<String>> headers = arguments.getHeaders();
        final String name = "X-HTTP-Method-Override";
        final Collection<String> overrides = headers.getOrDefault(name, emptyList());

        @Nullable final String override = overrides.stream().findFirst().orElse(null);

        if (override == null) {
            return null;
        }

        try {
            return HttpMethod.valueOf(override);
        } catch (final IllegalArgumentException e) {
            log.warn("Received invalid method in {} header: \"{}\"", name, override);
            return null;
        }
    }

}
