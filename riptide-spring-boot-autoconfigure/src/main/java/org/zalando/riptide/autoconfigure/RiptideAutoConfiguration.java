package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.Hack;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.apiguardian.api.API;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@Configuration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.zalando.logbook.autoconfigure.LogbookAutoConfiguration",
        "org.zalando.tracer.autoconfigure.TracerAutoConfiguration",
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

    @Hack("We need to 'rename' the existing meter registry so we can find it by name...")
    @Configuration
    @ConditionalOnClass(MeterRegistry.class)
    @AutoConfigureAfter(name = {
            "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration"
    })
    static class MetricsAutoConfiguration {

        @Bean
        @Primary
        @ConditionalOnBean({MeterRegistry.class, Clock.class})
        @ConditionalOnMissingBean(name = "meterRegistry")
        public CompositeMeterRegistry meterRegistry(final Clock clock, final List<MeterRegistry> registries) {
            return new CompositeMeterRegistry(clock, registries);
        }

    }

}
