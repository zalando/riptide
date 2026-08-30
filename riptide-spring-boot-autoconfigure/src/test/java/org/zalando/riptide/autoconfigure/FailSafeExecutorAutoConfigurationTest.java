package org.zalando.riptide.autoconfigure;


import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

import java.util.concurrent.ExecutorService;

@RiptideClientTest
@ActiveProfiles("default")
@Slf4j
public class FailSafeExecutorAutoConfigurationTest {

    @Configuration
    @ImportAutoConfiguration({
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldContainExecutorsConfiguredForFailSafePolicies() {
        final var consolidatedExecutor = applicationContext.getBean(
                "customExecutorTestFailsafeExecutorService", ExecutorService.class);
        Assertions.assertThat(consolidatedExecutor)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize", 2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 13);
    }

}
