package org.zalando.riptide.autoconfigure;

import lombok.*;

import javax.annotation.*;
import java.util.*;
import java.util.stream.*;

import static com.google.common.base.CaseFormat.*;
import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;

@AllArgsConstructor
final class Name {

    private final String id;
    private final String infix;
    private final Class<?>[] types;

    static Name name(final Class<?> type) {
        return name(null, type);
    }

    static Name name(@Nullable final String id, final Class<?>... types) {
        return name(id, null, types);
    }

    static Name name(@Nullable final String id, @Nullable final String infix, final Class<?>... types) {
        return new Name(id, infix, types);
    }

    Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    Set<String> getAlternatives() {
        return new LinkedHashSet<>(asList(toString(), toNormalizedString()));
    }

    @Override
    public String toString() {
        return toString(id);
    }

    String toNormalizedString() {
        if (id == null) {
            return toString();
        }

        return toString(hyphenToCamel(id));
    }

    private String toString(@Nullable final String id) {
        final Stream<String> parts = Stream.concat(
                Stream.of(id, infix).filter(Objects::nonNull),
                Stream.of(types).map(Class::getSimpleName));

        return parts.collect(collectingAndThen(joining(), this::upperToLowerCamel));
    }

    private String hyphenToCamel(final String s) {
        return LOWER_HYPHEN.to(LOWER_CAMEL, s);
    }

    private String upperToLowerCamel(final String s) {
        return UPPER_CAMEL.to(LOWER_CAMEL, s);
    }

}
