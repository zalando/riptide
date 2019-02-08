package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

final class StreamSpliteratorTest {

    private final JavaType type = SimpleType.constructUnsafe(List.class);

    private final JsonParser parser = mock(JsonParser.class);

    @Test
    void shouldNotSupportParallelExecution() {
        assertNull( new StreamSpliterator<>(type, parser).trySplit());
    }

    @Test
    void shouldNotPredictEstimateSize() {
        assertThat(new StreamSpliterator<>(type, parser).estimateSize(), is(Long.MAX_VALUE));
    }
}
