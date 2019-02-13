package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@Component
final class AsyncClientHttpRequestFactoryTest {

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
