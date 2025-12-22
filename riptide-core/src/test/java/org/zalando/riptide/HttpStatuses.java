package org.zalando.riptide;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.springframework.http.HttpStatus;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.*;


final class HttpStatuses {

    @SuppressWarnings("deprecation")
    private static final ImmutableSet<HttpStatus> DEPRECATED = Sets.immutableEnumSet(
            PROCESSING, // removed from spec
            PAYLOAD_TOO_LARGE, // duplicate with CONTENT_TOO_LARGE
            UNPROCESSABLE_ENTITY, // duplicate with UNPROCESSABLE_CONTENT
            I_AM_A_TEAPOT, // unused
            BANDWIDTH_LIMIT_EXCEEDED, // removed from spec
            NOT_EXTENDED // marked as historic, not in use
    );

    static Stream<HttpStatus> supported() {
        final Predicate<HttpStatus> isDeprecated = DEPRECATED::contains;
        return Stream.of(HttpStatus.values()).filter(isDeprecated.negate());
    }

}
