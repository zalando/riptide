package org.zalando.riptide;

import com.google.common.collect.ImmutableMultimap;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertNotSame;

final class RequesterImmutabilityTest {

    private final Http unit;

    RequesterImmutabilityTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
    }

    @Test
    void shouldNotReturnSameInstanceAfterMutation() {
        final QueryStage original = unit.get();
        assertNotSame(original, original.accept(MediaType.ALL));
        assertNotSame(original, original.contentType(MediaType.APPLICATION_JSON));
        assertNotSame(original, original.header("header","value"));
        assertNotSame(original, original.headers(ImmutableMultimap.of("header", "value")));
        assertNotSame(original, original.queryParam("p","v"));
        assertNotSame(original, original.queryParams(ImmutableMultimap.of("p", "v")));
        assertNotSame(original, original.ifMatch(""));
        assertNotSame(original, original.ifModifiedSince(OffsetDateTime.now()));
        assertNotSame(original, original.ifNoneMatch(""));
        assertNotSame(original, original.ifUnmodifiedSince(OffsetDateTime.now()));
    }

}
