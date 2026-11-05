package org.zalando.riptide.opentelemetry.span;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import org.zalando.riptide.RequestArguments;

import java.util.Optional;
import java.util.UUID;

import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.FLOW_ID;

public class FlowIdSpanDecorator implements SpanDecorator {

    public static final String FLOW_ID_HEADER = "X-Flow-ID";

    @Override
    public RequestArguments decorateRequest(final Span span, final RequestArguments arguments) {
        return arguments.withHeader(FLOW_ID_HEADER, flowId(span));
    }

    @Override
    public void onRequest(final Span span, final RequestArguments arguments) {
        // withHeader appends the Flow ID, so the last value is the one sent to the downstream service.
        arguments.getHeaders().getOrDefault(FLOW_ID_HEADER, java.util.List.of()).stream()
                .reduce((first, second) -> second)
                .ifPresent(flowId -> span.setAttribute(FLOW_ID, flowId));
    }

    private String flowId(final Span span) {
        return Optional.ofNullable(Baggage.current().getEntryValue(FLOW_ID.getKey()))
                .orElseGet(() -> span.getSpanContext().isValid()
                        ? span.getSpanContext().getTraceId()
                        // An invalid context reports a non-unique trace ID for every request.
                        : UUID.randomUUID().toString());
    }
}
