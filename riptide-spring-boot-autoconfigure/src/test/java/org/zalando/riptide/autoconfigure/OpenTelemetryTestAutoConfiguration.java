package org.zalando.riptide.autoconfigure;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryTestAutoConfiguration {
    @Bean
    public Tracer telemetryTracer() {
        return TracerProvider.noop().get("test");
    }
}
