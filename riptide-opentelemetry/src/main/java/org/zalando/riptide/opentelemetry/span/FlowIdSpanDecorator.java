package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.FLOW_ID;

@RequiredArgsConstructor
public class FlowIdSpanDecorator implements SpanDecorator {

    public static final String FLOW_ID_HEADER = "X-Flow-ID";

    private final boolean propagateFlowId;

    @Override
    public void onRequest(Span span, RequestArguments arguments) {
        Optional.ofNullable(Baggage.current().getEntryValue(FLOW_ID.getKey()))
            .ifPresent(flowId -> {
                span.setAttribute(FLOW_ID, flowId);

                if (propagateFlowId) {
                    arguments.withHeader(FLOW_ID_HEADER, flowId);
                }
            });
    }
}
