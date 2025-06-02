package org.zalando.riptide.autoconfigure;

import org.zalando.riptide.Plugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;
import org.zalando.riptide.opentelemetry.span.RetrySpanDecorator;
import org.zalando.riptide.opentelemetry.span.SpanDecorator;
import org.zalando.riptide.opentelemetry.span.StaticSpanDecorator;

import java.util.ArrayList;
import java.util.List;

final class OpenTelemetryPluginFactory {
    private OpenTelemetryPluginFactory() {

    }

    public static Plugin create(final RiptideProperties.Client client) {
        final List<SpanDecorator> decorators = new ArrayList<>();
        decorators.add(new StaticSpanDecorator(client.getTelemetry().getAttributes()));
        if (client.getRetry().getEnabled()) {
            decorators.add(new RetrySpanDecorator());
        }
        return new OpenTelemetryPlugin(decorators.toArray(new SpanDecorator[0]));
    }
}
