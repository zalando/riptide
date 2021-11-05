package org.zalando.riptide.autoconfigure;

import io.opentelemetry.api.trace.Tracer;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.opentelemetry.OpenTelemetryPlugin;

final class OpenTelemetryPluginFactory {
    private OpenTelemetryPluginFactory() {

    }

    public static Plugin create(final Tracer tracer) {
        return new OpenTelemetryPlugin(tracer);
    }
}
