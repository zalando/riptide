package org.zalando.riptide.autoconfigure;


import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.autoconfigure.RiptideProperties.BackupRequest;
import org.zalando.riptide.autoconfigure.RiptideProperties.CircuitBreaker;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.Defaults;
import org.zalando.riptide.autoconfigure.RiptideProperties.Retry;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;
import org.zalando.riptide.autoconfigure.RiptideProperties.Timeouts;

import java.net.URI;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public void shouldContainSingleExecutorConfiguredViaFailsafeThreads() {
        final var customExecutorTestFailsafeExecutorService = applicationContext.getBean("customExecutorTestFailsafeExecutorService", ExecutorService.class);
        Assertions.assertThat(customExecutorTestFailsafeExecutorService)
                .isNotNull()
                .hasFieldOrPropertyWithValue("corePoolSize", 2)
                .hasFieldOrPropertyWithValue("maximumPoolSize", 14);
    }

    @Test
    void shouldRejectLegacyRetryThreads() {
        final Client client = new Client();
        client.setRetry(new Retry(true, null, null, null, null, null, null, new Threads(true, 2, 4, null, null)));

        assertRejected("legacy-retry", client, "retry.threads");
    }

    @Test
    void shouldRejectLegacyCircuitBreakerThreads() {
        final Client client = new Client();
        client.setCircuitBreaker(new CircuitBreaker(true, null, null, null, null, new Threads(true, 2, 4, null, null)));

        assertRejected("legacy-circuit-breaker", client, "circuit-breaker.threads");
    }

    @Test
    void shouldRejectLegacyBackupRequestThreads() {
        final Client client = new Client();
        client.setBackupRequest(new BackupRequest(true, null, new Threads(true, 2, 4, null, null)));

        assertRejected("legacy-backup-request", client, "backup-request.threads");
    }

    @Test
    void shouldRejectLegacyTimeoutsThreads() {
        final Client client = new Client();
        client.setTimeouts(new Timeouts(true, null, new Threads(true, 2, 4, null, null)));

        assertRejected("legacy-timeouts", client, "timeouts.threads");
    }

    private void assertRejected(final String id, final Client client, final String expectedProperty) {
        final RiptideProperties properties = Defaulting.withDefaults(
                new RiptideProperties(new Defaults(), ImmutableMap.of(id, client)));
        properties.getClients().get(id).setBaseUrl(URI.create("http://example.com"));

        final DefaultRiptideRegistrar registrar = new DefaultRiptideRegistrar(
                new Registry(new SimpleBeanDefinitionRegistry()), properties);

        final LegacyFailsafeThreadsException exception = assertThrows(
                LegacyFailsafeThreadsException.class, registrar::register);

        assertEquals(id, exception.getClientId());
        assertEquals(expectedProperty, exception.getProperty());
        assertThat(exception.getMessage(), containsString("riptide.clients." + id + "." + expectedProperty));
        assertThat(exception.getMessage(), containsString("riptide.clients." + id + ".failsafe.threads"));
    }

}
