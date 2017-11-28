package org.zalando.riptide.spring;

import com.codahale.metrics.MetricRegistry;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.tracer.spring.TracerAutoConfiguration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
@AutoConfigureAfter(value = {
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        TracerAutoConfiguration.class,
})
public class RiptideAutoConfiguration {

    @Bean
    public static RiptidePostProcessor restClientPostProcessor() {
        return new RiptidePostProcessor(DefaultRiptideRegistrar::new);
    }

    @Configuration
    @ConditionalOnMissingBean(MetricsPlugin.class)
    @ConditionalOnBean(MetricRegistry.class)
    static class MetricsConfiguration {

        @Bean
        @SuppressWarnings("SpringJavaAutowiringInspection")
        public MetricsPlugin metricsPlugin(final MetricRegistry registry) {
            final MetricsGaugeService gaugeService = new MetricsGaugeService(registry);
            final ZMONMetricsNameGenerator nameGenerator = new ZMONMetricsNameGenerator();
            return new MetricsPlugin(gaugeService, nameGenerator);
        }

        @AllArgsConstructor
        private static class MetricsGaugeService implements GaugeService {

            private final MetricRegistry registry;

            @Override
            public void submit(final String metricName, final double value) {
                registry.timer(metricName).update((long) value, MILLISECONDS);
            }

        }

    }

}
