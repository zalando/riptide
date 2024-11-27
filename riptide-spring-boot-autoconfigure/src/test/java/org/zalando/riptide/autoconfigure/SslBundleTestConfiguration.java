package org.zalando.riptide.autoconfigure;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@Configuration
@ImportAutoConfiguration({
    SslAutoConfiguration.class,
    RiptideAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    LogbookAutoConfiguration.class,
    OpenTracingTestAutoConfiguration.class,
    OpenTracingFlowIdAutoConfiguration.class,
    MetricsTestAutoConfiguration.class,
})
public class SslBundleTestConfiguration {
}
