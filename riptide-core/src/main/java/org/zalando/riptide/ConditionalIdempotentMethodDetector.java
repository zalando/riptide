package org.zalando.riptide;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import org.apiguardian.api.API;

import javax.annotation.Nonnull;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://tools.ietf.org/html/rfc7232">Hypertext Transfer Protocol (HTTP/1.1): Conditional Requests</a>
 */
@API(status = EXPERIMENTAL)
public final class ConditionalIdempotentMethodDetector implements MethodDetector {

    private final ImmutableSet<String> conditionals = ImmutableSortedSet.copyOf(CASE_INSENSITIVE_ORDER, asList(
            /*
             * If-Match is most often used with state-changing methods (e.g., POST,
             * PUT, DELETE) to prevent accidental overwrites when multiple user
             * agents might be acting in parallel on the same resource (i.e., to
             * prevent the "lost update" problem).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.1
             */
            "If-Match",

            /*
             * If-None-Match can also be used with a value of "*" to prevent an
             * unsafe request method (e.g., PUT) from inadvertently modifying an
             * existing representation of the target resource when the client
             * believes that the resource does not have a current representation
             * (Section 4.2.1 of [RFC7231]).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.2
             */
            "If-None-Match",

            /*
             * If-Unmodified-Since is most often used with state-changing methods
             * (e.g., POST, PUT, DELETE) to prevent accidental overwrites when
             * multiple user agents might be acting in parallel on a resource that
             * does not supply entity-tags with its representations (i.e., to
             * prevent the "lost update" problem).
             *
             * https://tools.ietf.org/html/rfc7232#section-3.4
             */
            "If-Unmodified-Since"
    ));

    @Override
    public boolean test(@Nonnull final RequestArguments arguments) {
        return arguments.getHeaders().keySet().stream().anyMatch(conditionals::contains);
    }

}
