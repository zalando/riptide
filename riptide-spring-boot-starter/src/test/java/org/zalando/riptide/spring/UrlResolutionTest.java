package org.zalando.riptide.spring;

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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.Http;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.PassRoute.pass;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class UrlResolutionTest {

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
        public AsyncClientHttpRequestFactory exampleAsyncClientHttpRequestFactory(final AsyncRestTemplate template) {
            return template.getAsyncRequestFactory();
        }

    }

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    @Qualifier("example")
    private Http unit;

    @Test
    public void shouldAppendUrl() throws Throwable {
        server.expect(requestTo("https://example.com/foo/bar"))
                .andRespond(withSuccess());

        unit.get("/bar")
                .call(pass())
                .join();

        server.verify();
    }

}
