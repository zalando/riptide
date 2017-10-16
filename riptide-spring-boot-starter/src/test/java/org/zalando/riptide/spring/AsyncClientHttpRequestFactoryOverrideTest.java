package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.zalando.riptide.spring.Mocks.isMock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class AsyncClientHttpRequestFactoryOverrideTest {

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
    public void shouldOverride() {
        assertThat(unit, isMock());
    }

}
