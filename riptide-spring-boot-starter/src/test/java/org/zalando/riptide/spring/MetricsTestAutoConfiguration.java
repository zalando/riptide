package org.zalando.riptide.spring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MetricsTestAutoConfiguration {

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Configuration
    @ConditionalOnClass(name = "io.micrometer.spring.autoconfigure.CompositeMeterRegistryAutoConfiguration")
    @ImportAutoConfiguration
    public static class Spring4MetricsTestAutoConfiguration {

    }

    @Configuration
    @ConditionalOnClass(name = "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration")
    @ImportAutoConfiguration
    public static class Spring5MetricsTestAutoConfiguration {

    }

}
