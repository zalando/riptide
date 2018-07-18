package org.zalando.riptide.autoconfigure;

import org.apiguardian.api.API;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.MockRestServiceServer.MockRestServiceServerBuilder;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;

@API(status = EXPERIMENTAL)
@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideTestAutoConfiguration {

    static final String SERVER_BEAN_NAME = "mockRestServiceServer";
    static final String REST_TEMPLATE_BEAN_NAME = "_mockRestTemplate";
    static final String ASYNC_REST_TEMPLATE_BEAN_NAME = "_mockAsyncRestTemplate";

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

        @Bean(name = ASYNC_REST_TEMPLATE_BEAN_NAME)
        AsyncRestTemplate mockAsyncRestTemplate() {
            return new AsyncRestTemplate();
        }

        @Bean(name = SERVER_BEAN_NAME)
        MockRestServiceServer mockRestServiceServer(
                @Qualifier(REST_TEMPLATE_BEAN_NAME) final RestTemplate restTemplate,
                @Qualifier(ASYNC_REST_TEMPLATE_BEAN_NAME) final AsyncRestTemplate asyncRestTemplate)
                throws NoSuchFieldException, IllegalAccessException {

            final MockRestServiceServerBuilder builder = bindTo(restTemplate);
            bind(builder, asyncRestTemplate);
            return builder.build();
        }

        private void bind(final MockRestServiceServerBuilder builder, final AsyncRestTemplate asyncRestTemplate)
                throws NoSuchFieldException, IllegalAccessException {
            final Field field = builder.getClass().getDeclaredField("asyncRestTemplate");
            field.setAccessible(true);
            field.set(builder, asyncRestTemplate);
        }

    }

}
