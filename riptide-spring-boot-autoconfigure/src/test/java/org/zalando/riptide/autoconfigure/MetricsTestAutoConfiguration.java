package org.zalando.riptide.autoconfigure;

import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration({
        MetricsAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class
})
public class MetricsTestAutoConfiguration {

}
