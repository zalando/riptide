package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.zalando.riptide.RequestArguments;

import static org.mockito.Mockito.*;

class FlowIdSpanDecoratorTest {

    private static final String FLOW_ID = "flow-id-value";
    private Span span;
    private RequestArguments arguments;

    @BeforeEach
    void setUp() {
        span = mock(Span.class);
        arguments = mock(RequestArguments.class);
    }

    @Test
    void shouldSetFlowIdAttribute() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        final Baggage baggage = mock(Baggage.class);
        when(baggage.getEntryValue(ExtensionAttributes.FLOW_ID.getKey())).thenReturn(FLOW_ID);

        try (MockedStatic<Baggage> mockedBaggage = mockStatic(Baggage.class)) {
            mockedBaggage.when(Baggage::current).thenReturn(baggage);

            decorator.onRequest(span, arguments);

            verify(span).setAttribute(ExtensionAttributes.FLOW_ID, FLOW_ID);
        }
    }

    @Test
    void shouldDoNothingWhenFlowIdIsAbsent() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        final Baggage baggage = mock(Baggage.class);
        when(baggage.getEntryValue(ExtensionAttributes.FLOW_ID.getKey())).thenReturn(null);

        try (MockedStatic<Baggage> mockedBaggage = mockStatic(Baggage.class)) {
            mockedBaggage.when(Baggage::current).thenReturn(baggage);

            decorator.onRequest(span, arguments);

            verify(span, never()).setAttribute(anyString(), anyString());
            verify(arguments, never()).withHeader(anyString(), anyString());
        }
    }
}
