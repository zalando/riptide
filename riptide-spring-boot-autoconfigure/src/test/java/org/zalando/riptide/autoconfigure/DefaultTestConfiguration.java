package org.zalando.riptide.autoconfigure;

import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.zalando.logbook.autoconfigure.*;
import org.zalando.tracer.autoconfigure.*;

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
