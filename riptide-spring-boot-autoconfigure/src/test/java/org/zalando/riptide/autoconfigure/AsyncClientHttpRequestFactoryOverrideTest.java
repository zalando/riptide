package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@Component
final class AsyncClientHttpRequestFactoryOverrideTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        @Qualifier("example")
        public AsyncClientHttpRequestFactory exampleAsyncClientHttpRequestFactory() {
            return mock(AsyncClientHttpRequestFactory.class,
                    withSettings().extraInterfaces(ClientHttpRequestFactory.class));
        }

    }

    @Autowired
    @Qualifier("example")
    private AsyncClientHttpRequestFactory unit;

    @Test
    void shouldOverride() {
        assertThat(unit, Mocks.isMock());
    }

}
