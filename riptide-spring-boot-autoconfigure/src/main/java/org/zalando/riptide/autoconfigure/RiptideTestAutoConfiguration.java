package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;

import static org.apiguardian.api.API.Status.*;
import static org.springframework.test.web.client.MockRestServiceServer.*;

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
