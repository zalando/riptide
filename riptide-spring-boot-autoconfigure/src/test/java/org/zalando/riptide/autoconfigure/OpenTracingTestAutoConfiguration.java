package org.zalando.riptide.autoconfigure;

import io.opentracing.*;
import io.opentracing.mock.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.zalando.tracer.autoconfigure.*;

@Configuration
@AutoConfigureBefore(TracerAutoConfiguration.class)
public class OpenTracingTestAutoConfiguration {

    @Bean
    public Tracer sampleTracer() {
        return new MockTracer();
    }

}
