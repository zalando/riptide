package org.zalando.riptide.spring;

import org.apiguardian.api.API;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideTestAutoConfiguration {

    static final String SERVER_BEAN_NAME = "mockRestServiceServer";
    static final String TEMPLATE_BEAN_NAME = "_mockAsyncRestTemplate";

    @Bean
    public static RiptidePostProcessor restClientTestPostProcessor() {
        return new RiptidePostProcessor(TestRiptideRegistrar::new);
    }

    @Configuration
    static class MockConfiguration {

        @Bean(name = TEMPLATE_BEAN_NAME)
        AsyncRestTemplate mockAsyncRestTemplate() {
            return new AsyncRestTemplate();
        }

        @Bean(name = SERVER_BEAN_NAME)
        MockRestServiceServer mockRestServiceServer(@Qualifier(TEMPLATE_BEAN_NAME) final AsyncRestTemplate template) {
            return MockRestServiceServer.createServer(template);
        }

    }

}
