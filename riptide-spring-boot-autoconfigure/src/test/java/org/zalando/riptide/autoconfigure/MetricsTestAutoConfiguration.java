package org.zalando.riptide.autoconfigure;

import org.springframework.boot.actuate.autoconfigure.metrics.*;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;

@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
@ConditionalOnClass(CompositeMeterRegistryAutoConfiguration.class)
@ImportAutoConfiguration({
        MetricsAutoConfiguration.class,
        SimpleMetricsExportAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class
})
public class MetricsTestAutoConfiguration {

}
