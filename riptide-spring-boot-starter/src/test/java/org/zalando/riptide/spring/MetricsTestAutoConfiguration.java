package org.zalando.riptide.spring;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsTestAutoConfiguration {

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
