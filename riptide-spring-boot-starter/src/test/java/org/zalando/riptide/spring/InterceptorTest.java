package org.zalando.riptide.spring;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.faults.TransientFaultException;

import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class InterceptorTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public AsyncRestTemplate template() {
            return new AsyncRestTemplate();
        }

        @Bean
        public MockRestServiceServer server(final AsyncRestTemplate template) {
            return MockRestServiceServer.createServer(template);
        }

        @Bean
        @DependsOn("server")
        public ClientHttpRequestFactory githubClientHttpRequestFactory(final AsyncRestTemplate template) {
            return (ClientHttpRequestFactory) template.getAsyncRequestFactory();
        }

        @Bean
        @DependsOn("server")
        public AsyncClientHttpRequestFactory githubAsyncClientHttpRequestFactory(final AsyncRestTemplate template) {
            return template.getAsyncRequestFactory();
        }

    }

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("github")
    private RestTemplate github;

    @Autowired
    @Qualifier("github")
    private AsyncRestTemplate asyncGithub;

    @Test
    public void shouldSucceedToPerformSyncRequest() throws Exception {
        server.expect(requestTo("https://github.com/foo")).andRespond(withSuccess());

        github.getForEntity("/foo", Object.class);

        server.verify();
    }

    @Test(expected = TransientFaultException.class)
    public void shouldFailToPerformSyncRequest() throws Exception {
        server.expect(requestTo("https://github.com/foo")).andRespond(request -> {
            throw new SocketTimeoutException();
        });

        github.getForEntity("/foo", Object.class);
    }

    @Test
    public void shouldSucceedToPerformAsyncRequest() throws Exception {
        server.expect(requestTo("https://github.com/foo")).andRespond(withSuccess());

        asyncGithub.getForEntity("/foo", Object.class).get();

        server.verify();
    }

    @Test(expected = TransientFaultException.class)
    public void shouldFailToPerformAsyncRequest() throws Throwable {
        server.expect(requestTo("https://github.com/foo")).andRespond(request -> {
            throw new SocketTimeoutException();
        });

        try {
            asyncGithub.getForEntity("/foo", Object.class).get();
            Assert.fail("Expected exception");
        } catch (final ExecutionException e) {
            throw e.getCause();
        }
    }

}
