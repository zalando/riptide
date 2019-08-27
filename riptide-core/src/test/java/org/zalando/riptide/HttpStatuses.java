package org.zalando.riptide;

import com.google.common.collect.*;
import org.springframework.http.*;

import java.util.function.*;
import java.util.stream.*;

import static org.springframework.http.HttpStatus.*;

final class HttpStatuses {

    @SuppressWarnings("deprecation")
    private static final ImmutableSet<HttpStatus> DEPRECATED = Sets.immutableEnumSet(
            MOVED_TEMPORARILY, // duplicate with FOUND
            REQUEST_ENTITY_TOO_LARGE, // duplicate with PAYLOAD_TOO_LARGE
            REQUEST_URI_TOO_LONG // duplicate with URI_TOO_LONG
    );

    static Stream<HttpStatus> supported() {
        final Predicate<HttpStatus> isDeprecated = DEPRECATED::contains;
        return Stream.of(HttpStatus.values()).filter(isDeprecated.negate());
    }

}
