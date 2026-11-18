package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.opentelemetry.FlowIdPropagator;

import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.FLOW_ID;

public class FlowIdSpanDecorator implements SpanDecorator {

    /**
     * @deprecated Flow ID propagation is handled by {@code FlowIdPropagator}.
     */
    @Deprecated
    public static final String FLOW_ID_HEADER = FlowIdPropagator.FLOW_ID_HEADER;

    public FlowIdSpanDecorator() {
        // default constructor
    }

    /**
     * @deprecated Flow ID propagation is configured on {@code OpenTelemetryPlugin}; this decorator only adds the
     * Flow ID span attribute.
     */
    @Deprecated
    public FlowIdSpanDecorator(final boolean propagateFlowId) {
        this();
    }

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        final String flowId = Baggage.current().getEntryValue(FLOW_ID.getKey());
        if (flowId != null) {
            span.setAttribute(FLOW_ID, flowId);
        }
    }
}
