package org.zalando.riptide.spring;

import io.micrometer.core.instrument.MeterRegistry;
import org.apiguardian.api.API;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.zalando.riptide.metrics.MetricsPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;
import static org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.DEFAULT_TASK_SCHEDULER_BEAN_NAME;

@API(status = STABLE)
@Configuration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.zalando.logbook.spring.LogbookAutoConfiguration",
        "org.zalando.tracer.spring.TracerAutoConfiguration",
        "org.zalando.tracer.spring.TracerSchedulingAutoConfiguration",  // only needed for tracer < 0.12.0,
        "io.micrometer.spring.autoconfigure.CompositeMeterRegistryAutoConfiguration", // Spring Boot 1.x
        "org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration", // Spring Boot 2.x
})
@AutoConfigureBefore(name = {
        "org.springframework.scheduling.annotation.SchedulingConfiguration",
        "org.zalando.failsafeactuator.config.FailsafeInjectionConfiguration"
})
public class RiptideAutoConfiguration {

    @API(status = INTERNAL)
    @Bean
    public static RiptidePostProcessor restClientPostProcessor() {
        return new RiptidePostProcessor(DefaultRiptideRegistrar::new);
    }

    @Configuration
    @ConditionalOnClass(MetricsPlugin.class)
    @ConditionalOnMissingBean(MetricsPlugin.class)
    @ConditionalOnBean(MeterRegistry.class)
    static class MetricsConfiguration {

        @Bean
        @SuppressWarnings("SpringJavaAutowiringInspection")
        public MetricsPlugin metricsPlugin(final MeterRegistry registry) {
            return new MetricsPlugin(registry);
        }

    }

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
