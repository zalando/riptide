package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IdempotencyKeyIdempotentMethodDetectorTest {

    private final MethodDetector unit = new IdempotencyKeyIdempotentMethodDetector();

    @Test
    void shouldDetectIdempotencyKey() {
        assertTrue(unit.test(RequestArguments.create()
            .withHeaders(ImmutableMultimap.of("Idempotency-Key", UUID.randomUUID().toString()))));
    }

    @Test
    void shouldDetectIdempotencyKeyCaseInsensitive() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeaders(ImmutableMultimap.of("idempotency-key", UUID.randomUUID().toString()))));
    }

    @Test
    void shouldNotDetectAbsentIdempotencyKey() {
        assertFalse(unit.test(RequestArguments.create()));
    }

}
