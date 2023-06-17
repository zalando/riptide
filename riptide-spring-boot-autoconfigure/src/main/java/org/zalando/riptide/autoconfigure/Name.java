package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.joining;

@AllArgsConstructor
final class Name {

    private final String id;
    private final String infix;
    private final Class<?>[] types;

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
