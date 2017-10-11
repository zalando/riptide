package org.zalando.riptide.spring;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.tracer.spring.TracerAutoConfiguration;

@Configuration
@AutoConfigureAfter(value = {
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        TracerAutoConfiguration.class,
}, name = {
        "ZmonMetricFilterAutoConfiguration"
})
public class RestClientAutoConfiguration {

    @Bean
    public static RestClientPostProcessor restClientPostProcessor() {
        return new RestClientPostProcessor();
    }

}
