package org.zalando.riptide;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ContentTypeComparatorTest {

    private final Comparator<MediaType> comparator =
            ContentTypeNavigator.SPECIFIC_COMPARATOR;

    @Test
    void shouldReturnZeroForEqualMediaTypes() {
        assertEquals(0,
                comparator.compare(
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON
                )
        );
    }

    @Test
    void shouldPreferConcreteSubtypeOverWildcard() {
        // application/json is more specific than application/*
        assertEquals(1,
                comparator.compare(
                        MediaType.parseMediaType("application/*"),
                        MediaType.APPLICATION_JSON
                )
        );

        assertEquals(-1,
                comparator.compare(
                        MediaType.APPLICATION_JSON,
                        MediaType.parseMediaType("application/*")
                )
        );
    }

    @Test
    void shouldPreferConcreteTypeOverWildcardType() {
        // application/problem+json is more specific than */*
        assertEquals(1,
                comparator.compare(
                        MediaType.ALL,
                        MediaType.APPLICATION_PROBLEM_JSON
                )
        );

        assertEquals(-1,
                comparator.compare(
                        MediaType.APPLICATION_PROBLEM_JSON,
                        MediaType.ALL
                )
        );
    }
}
