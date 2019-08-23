package org.zalando.riptide.opentracing.span;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.lang.String.join;

final class HeaderExtractor {

    Optional<String> extract(
            final Map<String, List<String>> headers,
            final List<String> names) {

        return names.stream()
                .map(headers::get)
                .filter(Objects::nonNull)
                .filter(not(List::isEmpty))
                .findFirst()
                .map(list -> join("\n", list));
    }

    private static <T> Predicate<T> not(final Predicate<T> predicate) {
        return predicate.negate();
    }

}
