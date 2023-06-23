package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.API;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@AutoConfiguration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.zalando.logbook.autoconfigure.LogbookAutoConfiguration",
        "org.zalando.riptide.autoconfigure.OpenTracingFlowIdAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
})
@AutoConfigureBefore(name = {
        "org.zalando.actuate.autoconfigure.failsafe.CircuitBreakersEndpointAutoConfiguration"
})
public class RiptideAutoConfiguration {

    @API(status = INTERNAL)
    @Bean
    public static RiptidePostProcessor riptidePostProcessor() {
        return new RiptidePostProcessor(DefaultRiptideRegistrar::new);
    }

}
