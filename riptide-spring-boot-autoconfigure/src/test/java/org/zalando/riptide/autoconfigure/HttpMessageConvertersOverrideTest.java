package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.stereotype.Component;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@Component
final class HttpMessageConvertersOverrideTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        @Qualifier("github-invalid")
        public ClientHttpMessageConverters githubInvalidHttpMessageConverters() {
            return new ClientHttpMessageConverters(singletonList(new Jaxb2RootElementHttpMessageConverter()));
        }

    }

    @Autowired
    @Qualifier("github-invalid")
    private ClientHttpMessageConverters unit;

    @Test
    void shouldOverride() {
        assertThat(unit.getConverters(), contains(instanceOf(Jaxb2RootElementHttpMessageConverter.class)));
    }

}
