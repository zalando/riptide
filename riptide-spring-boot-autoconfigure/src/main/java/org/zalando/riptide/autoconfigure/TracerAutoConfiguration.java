package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import org.springframework.context.annotation.Bean;

public class TracerAutoConfiguration {
    @Bean
    public Tracer tracer() {
        return NoopTracerFactory.create();
    }
}
