package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(OpenTracingFlowIdAutoConfiguration.class)
public class OpenTracingTestAutoConfiguration {

    @Bean
    public Tracer sampleTracer() {
        return new MockTracer();
    }

}
