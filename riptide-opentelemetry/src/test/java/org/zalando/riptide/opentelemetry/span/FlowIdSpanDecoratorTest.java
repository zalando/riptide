package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.zalando.riptide.RequestArguments;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;

class FlowIdSpanDecoratorTest {

    private static final String FLOW_ID = "flow-id-value";
    private static final String TRACE_ID = "trace-id-value";
    private static final String FLOW_ID_HEADER = "X-Flow-ID";

    private Span span;
    private SpanContext spanContext;
    private RequestArguments arguments;

    @BeforeEach
    void setUp() {
        span = mock(Span.class);
        arguments = mock(RequestArguments.class);

        spanContext = mock(SpanContext.class);
        when(span.getSpanContext()).thenReturn(spanContext);
        when(spanContext.isValid()).thenReturn(true);
        when(spanContext.getTraceId()).thenReturn(TRACE_ID);
    }

    @Test
    void shouldUseBaggageFlowIdForHeaderWhenAvailable() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();

        withBaggage(FLOW_ID, () -> decorator.decorateRequest(span, arguments));

        verify(arguments).withHeader(FLOW_ID_HEADER, FLOW_ID);
    }

    @Test
    void shouldUseBaggageFlowIdForHeaderWhenSpanContextIsInvalid() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        when(spanContext.isValid()).thenReturn(false);

        withBaggage(FLOW_ID, () -> decorator.decorateRequest(span, arguments));

        verify(arguments).withHeader(FLOW_ID_HEADER, FLOW_ID);
        verify(spanContext, never()).getTraceId();
    }

    @Test
    void shouldUseTraceIdForHeaderWhenBaggageFlowIdIsAbsentAndSpanContextIsValid() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();

        withBaggage(null, () -> decorator.decorateRequest(span, arguments));

        verify(arguments).withHeader(FLOW_ID_HEADER, TRACE_ID);
    }

    @Test
    void shouldUseRandomFlowIdForHeaderWhenBaggageFlowIdIsAbsentAndSpanContextIsInvalid() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        when(span.getSpanContext().isValid()).thenReturn(false);

        withBaggage(null, () -> decorator.decorateRequest(span, arguments));

        final org.mockito.ArgumentCaptor<String> flowId = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(arguments).withHeader(eq(FLOW_ID_HEADER), flowId.capture());
        assertThat(UUID.fromString(flowId.getValue()), notNullValue());
    }

    @Test
    void shouldSetFlowIdAttributeFromRequestHeader() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        when(arguments.getHeaders()).thenReturn(Map.of(FLOW_ID_HEADER, List.of("previous-flow-id", FLOW_ID)));

        decorator.onRequest(span, arguments);

        verify(span).setAttribute(ExtensionAttributes.FLOW_ID, FLOW_ID);
    }

    @Test
    void shouldSetSameRandomFlowIdOnSpanAsInRequestHeader() {
        final FlowIdSpanDecorator decorator = new FlowIdSpanDecorator();
        final RequestArguments originalArguments = RequestArguments.create();
        when(spanContext.isValid()).thenReturn(false);

        final Baggage baggage = mock(Baggage.class);
        when(baggage.getEntryValue(ExtensionAttributes.FLOW_ID.getKey())).thenReturn(null);

        try (MockedStatic<Baggage> mockedBaggage = mockStatic(Baggage.class)) {
            mockedBaggage.when(Baggage::current).thenReturn(baggage);

            final RequestArguments decoratedArguments = decorator.decorateRequest(span, originalArguments);
            final String flowId = decoratedArguments.getHeaders().get(FLOW_ID_HEADER).get(0);
            decorator.onRequest(span, decoratedArguments);

            assertThat(UUID.fromString(flowId), notNullValue());
            verify(span).setAttribute(ExtensionAttributes.FLOW_ID, flowId);
        }
    }

    private static void withBaggage(final String flowId, final Runnable action) {
        final Baggage baggage = mock(Baggage.class);
        when(baggage.getEntryValue(ExtensionAttributes.FLOW_ID.getKey())).thenReturn(flowId);

        try (MockedStatic<Baggage> mockedBaggage = mockStatic(Baggage.class)) {
            mockedBaggage.when(Baggage::current).thenReturn(baggage);
            action.run();
        }
    }
}
