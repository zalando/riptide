package org.zalando.riptide.autoconfigure;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.tracer.autoconfigure.TracerAutoConfiguration;

@Configuration
@ImportAutoConfiguration({
        RiptideAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        TracerAutoConfiguration.class,
        OpenTracingTestAutoConfiguration.class,
        MetricsTestAutoConfiguration.class,
})
@ActiveProfiles("default")
public class DefaultTestConfiguration {

}
