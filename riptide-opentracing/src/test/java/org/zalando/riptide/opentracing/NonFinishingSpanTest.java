package org.zalando.riptide.opentracing;

import io.opentracing.Span;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

final class NonFinishingSpanTest {

    @Test
    void shouldNotDelegateFinishMicroseconds() {
        final Span span = spy(Span.class);
        final Span unit = new NonFinishingSpan(span);

        unit.finish(0);

        verifyNoInteractions(span);
    }

}
