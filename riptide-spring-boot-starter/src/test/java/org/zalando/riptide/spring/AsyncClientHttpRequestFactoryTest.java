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
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;
import org.zalando.stups.tokens.AccessTokens;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class AsyncClientHttpRequestFactoryTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public AccessTokens accessTokens() {
            return mock(AccessTokens.class);
        }

    }

    @Autowired
    @Qualifier("example")
    private ClientHttpRequestFactory sync;

    @Autowired
    @Qualifier("example")
    private AsyncClientHttpRequestFactory async;

    @Test
    public void shouldAutowireSync() {
        assertThat(sync.getClass(), is(RestAsyncClientHttpRequestFactory.class));
    }

    @Test
    public void shouldAutowireAsync() {
        assertThat(async.getClass(), is(RestAsyncClientHttpRequestFactory.class));
    }

}
