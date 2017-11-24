package org.zalando.riptide.spring.testing;


import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.zalando.riptide.spring.RiptideAutoConfiguration;

import java.lang.reflect.Constructor;

@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideMockRestServiceServerAutoConfiguration {

    @Bean
    public RequestExpectationManager requestExpectationManager() {
        return new SimpleRequestExpectationManager();
    }

    @Bean
    public MockRestServiceServer mockRestServiceServer(final RequestExpectationManager manager) throws Exception {
        final Constructor<MockRestServiceServer> ctr = MockRestServiceServer.class.getDeclaredConstructor(RequestExpectationManager.class);
        ctr.setAccessible(true);
        return ctr.newInstance(manager);
    }

}
