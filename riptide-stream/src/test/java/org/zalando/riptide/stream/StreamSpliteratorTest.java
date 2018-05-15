package org.zalando.riptide.stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public final class StreamSpliteratorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final JavaType type = SimpleType.constructUnsafe(List.class);

    @Mock
    private JsonParser parser;

    @Test
    public void shouldNotSupportParallelExecution() {
        assertNull( new StreamSpliterator<>(type, parser).trySplit());
    }

    @Test
    public void shouldNotPredictEstimateSize() {
        assertThat(new StreamSpliterator<>(type, parser).estimateSize(), is(Long.MAX_VALUE));
    }
}
