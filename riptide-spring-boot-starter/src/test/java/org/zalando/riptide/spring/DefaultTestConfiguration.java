package org.zalando.riptide.spring;

import com.codahale.metrics.MetricRegistry;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.riptide.exceptions.TemporaryExceptionPlugin;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.tracer.spring.TracerAutoConfiguration;
import org.zalando.zmon.actuator.config.ZmonMetricsAutoConfiguration;

import static org.mockito.Mockito.mock;
import static org.zalando.riptide.exceptions.ExceptionClassifier.create;

@Configuration
@ImportAutoConfiguration({
        RestClientAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        TracerAutoConfiguration.class,
        ZmonMetricsAutoConfiguration.class
})
@ActiveProfiles("default")
public class DefaultTestConfiguration {

    @Bean
    public MetricRegistry metricRegistry() {
        return mock(MetricRegistry.class);
    }

    @Bean
    public AccessTokens accessTokens() {
        return mock(AccessTokens.class);
    }

    @Bean
    public TemporaryExceptionPlugin temporaryExceptionPlugin() {
        return new TemporaryExceptionPlugin(create(IllegalStateException.class::isInstance));
    }

}
