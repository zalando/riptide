package org.zalando.riptide.spring;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Throwables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.Matchers.containsString;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.compose;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.mockito.Mockito.mock;

public final class AccessTokensMissingTest {

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {

        @Bean
        @Primary
        public HttpMessageConverters httpMessageConverters() {
            return new HttpMessageConverters();
        }

        @Bean
        public MetricRegistry metricRegistry() {
            return mock(MetricRegistry.class);
        }

    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFailDueToMissingAccessTokenUrl() {
        exception.expect(hasFeature(Throwables::getRootCause,
                hasFeature("message", Throwable::getMessage,
                        compose(containsString("riptide.oauth.access-token-url")).and(
                                containsString("ACCESS_TOKEN_URL")))));

        new SpringApplicationBuilder(TestConfiguration.class)
                .profiles("default")
                .build()
                .run();
    }

}
