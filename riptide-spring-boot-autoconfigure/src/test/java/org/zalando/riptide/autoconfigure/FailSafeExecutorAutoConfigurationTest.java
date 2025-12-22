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
//            LogbookAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    static class ContextConfiguration {
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void shouldContainExecutorsConfiguredForFailSafePolicies() {
        final var customExecutorTestRetryPolicyExecutorService = applicationContext.getBean("customExecutorTestRetryPolicyExecutorService");
        final var customExecutorTestCircuitBreakerExecutorService = applicationContext.getBean("customExecutorTestCircuitBreakerExecutorService", ExecutorService.class);
        final var customExecutorTestBackupRequestExecutorService = applicationContext.getBean("customExecutorTestBackupRequestExecutorService", ExecutorService.class);
        final var customExecutorTestTimeoutExecutorService = applicationContext.getBean("customExecutorTestTimeoutExecutorService", ExecutorService.class);
        Assertions.assertThat(customExecutorTestRetryPolicyExecutorService)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize",2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 13);
        Assertions.assertThat(customExecutorTestCircuitBreakerExecutorService)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize",2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 10);
        Assertions.assertThat(customExecutorTestBackupRequestExecutorService)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize",2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 12);
        Assertions.assertThat(customExecutorTestTimeoutExecutorService)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize",2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 11);
    }

}
