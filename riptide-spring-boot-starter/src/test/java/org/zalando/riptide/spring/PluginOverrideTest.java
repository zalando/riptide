package org.zalando.riptide.spring;

import com.codahale.metrics.MetricRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.stups.tokens.AccessTokens;

import static org.mockito.Mockito.mock;

public final class PluginOverrideTest {

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {

        @Bean
        public MetricRegistry metricRegistry() {
            return mock(MetricRegistry.class);
        }

        @Bean
        public AccessTokens accessTokens() {
            return mock(AccessTokens.class);
        }

    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFailDueToUnknownModule() {
        exception.expectMessage("Unknown plugin name: bar");

        new SpringApplicationBuilder(AccessTokensMissingTest.TestConfiguration.class)
                .profiles("plugin")
                .build()
                .run();
    }

}
