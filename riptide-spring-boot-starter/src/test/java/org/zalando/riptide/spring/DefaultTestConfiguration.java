package org.zalando.riptide.spring;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.tracer.spring.TracerAutoConfiguration;

@Configuration
@ImportAutoConfiguration({
        RiptideAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        TracerAutoConfiguration.class,
        MetricsTestAutoConfiguration.class,
})
@ActiveProfiles("default")
public class DefaultTestConfiguration {

}
