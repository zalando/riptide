package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

@SpringBootTest(webEnvironment = NONE)
@Component
final class ClientCustomizationTest {

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
    void shouldCustomize() {
        verify(customizer).customize(any());

    }

}
