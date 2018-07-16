package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class IdempotencyKeyIdempotentMethodDetectorTest {

    private final MethodDetector unit = new IdempotencyKeyIdempotentMethodDetector();

    @Test
    public void shouldDetectIdempotencyKey() {
        assertTrue(unit.test(RequestArguments.create()
            .withHeaders(ImmutableMultimap.of("Idempotency-Key", UUID.randomUUID().toString()))));
    }

    @Test
    public void shouldDetectIdempotencyKeyCaseInsensitive() {
        assertTrue(unit.test(RequestArguments.create()
                .withHeaders(ImmutableMultimap.of("idempotency-key", UUID.randomUUID().toString()))));
    }

    @Test
    public void shouldNotDetectAbsentIdempotencyKey() {
        assertFalse(unit.test(RequestArguments.create()));
    }

}
