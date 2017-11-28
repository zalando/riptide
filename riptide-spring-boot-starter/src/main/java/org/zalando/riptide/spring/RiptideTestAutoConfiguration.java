package org.zalando.riptide.spring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideTestAutoConfiguration {

    private static final String SERVER_BEAN_NAME = "mockRestServiceServer";
    private static final String TEMPLATE_BEAN_NAME = "_mockAsyncRestTemplate";

    @Bean
    public static RiptidePostProcessor restClientTestPostProcessor() {
        return new RiptidePostProcessor((registry, settings) ->
                new TestRiptideRegistrar(registry, settings, TEMPLATE_BEAN_NAME, SERVER_BEAN_NAME));
    }

    @Configuration
    static class MockConfiguration {

        @Bean(name = TEMPLATE_BEAN_NAME)
        AsyncRestTemplate mockAsyncRestTemplate() {
            return new AsyncRestTemplate();
        }

        @Bean(name = SERVER_BEAN_NAME)
        MockRestServiceServer mockRestServiceServer(@Qualifier("_mockAsyncRestTemplate") final AsyncRestTemplate template) {
            return MockRestServiceServer.createServer(template);
        }

    }

}
