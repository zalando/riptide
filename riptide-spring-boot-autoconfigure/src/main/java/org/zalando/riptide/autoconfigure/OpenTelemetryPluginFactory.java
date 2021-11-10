package org.zalando.riptide.autoconfigure;

import io.opentelemetry.api.trace.Tracer;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;
import org.zalando.riptide.opentelemetry.span.StaticSpanDecorator;

final class OpenTelemetryPluginFactory {
    private OpenTelemetryPluginFactory() {

    }

    public static Plugin create(final Tracer tracer, final RiptideProperties.Client client) {
        StaticSpanDecorator decorator = new StaticSpanDecorator(client.getTelemetry().getAttributes());
        return new OpenTelemetryPlugin(tracer, decorator);
    }
}
