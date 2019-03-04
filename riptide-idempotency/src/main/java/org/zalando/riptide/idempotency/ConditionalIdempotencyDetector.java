package org.zalando.riptide.idempotency;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7232">Hypertext Transfer Protocol (HTTP/1.1): Conditional Requests</a>
 */
@API(status = EXPERIMENTAL)
public final class ConditionalIdempotencyDetector implements IdempotencyDetector {

    private final Map<String, Predicate<String>> conditionals = ImmutableMap.of(
            /*
             * If-Match is most often used with state-changing methods (e.g., POST,
             * PUT, DELETE) to prevent accidental overwrites when multiple user
             * agents might be acting in parallel on the same resource (i.e., to
             * prevent the "lost update" problem).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.1
             *
             * Excludes * and requires a single entity tag, because otherwise it
             * wouldn't be idempotent.
             */
            "If-Match", CharMatcher.anyOf("*,")::matchesNoneOf,

            /*
             * If-None-Match can also be used with a value of "*" to prevent an
             * unsafe request method (e.g., PUT) from inadvertently modifying an
             * existing representation of the target resource when the client
             * believes that the resource does not have a current representation
             * (Section 4.2.1 of [RFC7231]).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.2
             *
             * Requires *, because any other usage wouldn't be idempotent.
             */
            "If-None-Match", "*"::equals,

            /*
             * If-Unmodified-Since is most often used with state-changing methods
             * (e.g., POST, PUT, DELETE) to prevent accidental overwrites when
             * multiple user agents might be acting in parallel on a resource that
             * does not supply entity-tags with its representations (i.e., to
             * prevent the "lost update" problem).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.4
             */
            "If-Unmodified-Since", $ -> true
    );

    @Override
    public boolean test(final RequestArguments arguments, final Predicate<RequestArguments> root) {
        final Map<String, List<String>> headers = arguments.getHeaders();
        return conditionals.entrySet().stream()
                .anyMatch(entry -> {
                    final String name = entry.getKey();
                    final Predicate<String> predicate = entry.getValue();
                    final List<String> values = headers.getOrDefault(name, emptyList());
                    return values.stream().anyMatch(predicate);
                });
    }

}
