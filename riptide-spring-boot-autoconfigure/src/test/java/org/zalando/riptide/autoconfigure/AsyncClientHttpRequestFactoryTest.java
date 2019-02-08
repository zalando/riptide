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
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.stups.tokens.AccessTokens;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@Component
final class AsyncClientHttpRequestFactoryTest {

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
    void shouldAutowireSync() {
        assertThat(sync.getClass(), is(ApacheClientHttpRequestFactory.class));
    }

    @Test
    void shouldAutowireAsync() {
        assertThat(async.getClass(), is(ConcurrentClientHttpRequestFactory.class));
    }

}
