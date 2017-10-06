package org.zalando.riptide.spring;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    public static RestClientPostProcessor restClientPostProcessor(final ObjectFactory<PluginResolver> resolver) {
        return new RestClientPostProcessor(resolver);
    }

    @Bean
    @ConditionalOnMissingBean(PluginResolver.class)
    public PluginResolver pluginResolver(final ListableBeanFactory factory) {
        return new DefaultPluginResolver(factory);
    }

}
