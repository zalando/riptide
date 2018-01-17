package org.zalando.riptide.spring;

import io.micrometer.core.instrument.MeterRegistry;
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

import static org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.DEFAULT_TASK_SCHEDULER_BEAN_NAME;

@Configuration
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration",
        "org.zalando.logbook.spring.LogbookAutoConfiguration",
        "org.zalando.tracer.spring.TracerAutoConfiguration",
        "org.zalando.tracer.spring.TracerSchedulingAutoConfiguration",  // only needed for tracer < 0.12.0
})
@AutoConfigureBefore(name = "org.springframework.scheduling.annotation.SchedulingConfiguration")
public class RiptideAutoConfiguration {

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
            final ZMONMetricsNameGenerator nameGenerator = new ZMONMetricsNameGenerator();
            return new MetricsPlugin(registry, nameGenerator);
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
