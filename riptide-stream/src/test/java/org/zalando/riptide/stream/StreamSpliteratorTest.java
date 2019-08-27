package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
