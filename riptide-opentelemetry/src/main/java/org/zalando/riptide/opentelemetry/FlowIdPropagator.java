package org.zalando.riptide.opentelemetry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singleton;
import static org.zalando.riptide.opentelemetry.span.ExtensionAttributes.FLOW_ID;

public final class FlowIdPropagator implements TextMapPropagator {

    public static final String FLOW_ID_HEADER = "X-Flow-ID";

    @Override
    public Collection<String> fields() {
        return singleton(FLOW_ID_HEADER);
    }

    @Override
    public <C> void inject(final Context context, final C carrier, final TextMapSetter<C> setter) {
        Optional.ofNullable(Baggage.fromContext(context).getEntryValue(FLOW_ID.getKey()))
                .ifPresent(flowId -> setter.set(carrier, FLOW_ID_HEADER, flowId));
    }

    @Override
    public <C> Context extract(final Context context, final C carrier, final TextMapGetter<C> getter) {
        return context;
    }
}
