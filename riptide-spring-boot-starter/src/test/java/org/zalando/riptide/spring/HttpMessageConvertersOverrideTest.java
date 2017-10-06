package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class HttpMessageConvertersOverrideTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        @Qualifier("example")
        public ClientHttpMessageConverters exampleHttpMessageConverters() {
            return new ClientHttpMessageConverters(singletonList(new Jaxb2RootElementHttpMessageConverter()));
        }

    }

    @Autowired
    @Qualifier("example")
    private ClientHttpMessageConverters unit;

    @Test
    public void shouldOverride() {
        assertThat(unit.getConverters(), contains(instanceOf(Jaxb2RootElementHttpMessageConverter.class)));
    }

}
