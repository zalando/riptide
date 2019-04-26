package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.API;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.test.web.client.MockRestServiceServer.createServer;

@API(status = EXPERIMENTAL)
@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideTestAutoConfiguration {

    static final String SERVER_BEAN_NAME = "mockRestServiceServer";
    static final String REST_TEMPLATE_BEAN_NAME = "_mockRestTemplate";

    @Bean
    public static RiptidePostProcessor restClientTestPostProcessor() {
        return new RiptidePostProcessor(TestRiptideRegistrar::new);
    }

    @Configuration
    static class MockConfiguration {

        @Bean(name = REST_TEMPLATE_BEAN_NAME)
        RestTemplate mockRestTemplate() {
            return new RestTemplate();
        }

        @Bean(name = SERVER_BEAN_NAME)
        MockRestServiceServer mockRestServiceServer(
                @Qualifier(REST_TEMPLATE_BEAN_NAME) final RestTemplate restTemplate) {

            return createServer(restTemplate);
        }

    }

}
