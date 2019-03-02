package org.zalando.riptide;

import com.google.common.collect.ImmutableMap;
import org.apiguardian.api.API;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7232">Hypertext Transfer Protocol (HTTP/1.1): Conditional Requests</a>
 */
@API(status = EXPERIMENTAL)
public final class ConditionalIdempotentMethodDetector implements MethodDetector {

    private final Map<String, Predicate<String>> conditionals = ImmutableMap.of(
            /*
             * If-Match is most often used with state-changing methods (e.g., POST,
             * PUT, DELETE) to prevent accidental overwrites when multiple user
             * agents might be acting in parallel on the same resource (i.e., to
             * prevent the "lost update" problem).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.1
             */
            "If-Match", $ -> true,

            /*
             * If-None-Match can also be used with a value of "*" to prevent an
             * unsafe request method (e.g., PUT) from inadvertently modifying an
             * existing representation of the target resource when the client
             * believes that the resource does not have a current representation
             * (Section 4.2.1 of [RFC7231]).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.2
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
    public boolean test(final RequestArguments arguments) {
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
