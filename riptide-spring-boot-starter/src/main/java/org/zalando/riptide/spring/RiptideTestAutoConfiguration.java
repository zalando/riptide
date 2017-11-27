package org.zalando.riptide.spring;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(RiptideAutoConfiguration.class)
public class RiptideTestAutoConfiguration {

    @Bean
    public static RiptidePostProcessor restClientTestPostProcessor() {
        return new RiptidePostProcessor(TestRiptideRegistrar::new);
    }

}
