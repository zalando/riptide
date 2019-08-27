package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.xml.*;
import org.springframework.stereotype.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

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
