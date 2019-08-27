package org.zalando.riptide.autoconfigure;

import com.google.gag.annotation.remark.*;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.composite.*;
import org.apiguardian.api.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;

import java.util.*;
import java.util.concurrent.*;

import static org.apiguardian.api.API.Status.*;
import static org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.*;

@API(status = STABLE)
@Configuration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.zalando.logbook.autoconfigure.LogbookAutoConfiguration",
        "org.zalando.tracer.autoconfigure.TracerAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration",
})
@AutoConfigureBefore(name = {
        "org.springframework.scheduling.annotation.SchedulingConfiguration",
        "org.zalando.actuate.autoconfigure.failsafe.CircuitBreakersEndpointAutoConfiguration"
})
public class RiptideAutoConfiguration {

    @API(status = INTERNAL)
    @Bean
    public static RiptidePostProcessor restClientPostProcessor() {
        return new RiptidePostProcessor(DefaultRiptideRegistrar::new);
    }

    @Hack("We need to 'rename' the existing meter registry so we can find it by name...")
    @Configuration
    @ConditionalOnClass(MeterRegistry.class)
    static class MetricsAutoConfiguration {

        @Bean
        @Primary
        @ConditionalOnBean({MeterRegistry.class, Clock.class})
        @ConditionalOnMissingBean(name = "meterRegistry")
        public CompositeMeterRegistry meterRegistry(final Clock clock, final List<MeterRegistry> registries) {
            return new CompositeMeterRegistry(clock, registries);
        }

    }

    // needed because @Scheduled would silently use the single-thread scheduler needed for retries/circuit breakers
    // see https://github.com/zalando/riptide/issues/319 for details
    @Configuration
    @ConditionalOnClass(Scheduled.class)
    static class SchedulingAutoConfiguration {

        @Bean(name = DEFAULT_TASK_SCHEDULER_BEAN_NAME, destroyMethod = "shutdown")
        @ConditionalOnMissingBean(name = DEFAULT_TASK_SCHEDULER_BEAN_NAME)
        public ScheduledExecutorService taskScheduler() {
            final int corePoolSize = Runtime.getRuntime().availableProcessors();
            return Executors.newScheduledThreadPool(corePoolSize);
        }

    }

}
