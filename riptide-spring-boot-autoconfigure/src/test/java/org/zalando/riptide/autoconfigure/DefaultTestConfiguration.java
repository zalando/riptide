package org.zalando.riptide.autoconfigure;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@Configuration
@ImportAutoConfiguration({
        RiptideAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        LogbookAutoConfiguration.class,
        OpenTracingTestAutoConfiguration.class,
        OpenTracingFlowIdAutoConfiguration.class,
        MetricsTestAutoConfiguration.class,
})
@ActiveProfiles("default")
public class DefaultTestConfiguration {

}
