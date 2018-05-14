package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class ClientCustomizationTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        @Qualifier("example")
        public HttpClientCustomizer exampleHttpClientCustomizer() {
            return mock(HttpClientCustomizer.class);
        }

    }

    @Autowired
    @Qualifier("example")
    private HttpClientCustomizer customizer;

    @Test
    public void shouldCustomize() {
        verify(customizer).customize(any());

    }

}
