package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;
import static org.zalando.riptide.autoconfigure.Mocks.*;

@SpringBootTest(webEnvironment = NONE)
@Component
final class ObjectMapperOverrideTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

        @Bean
        @Primary
        public ObjectMapper jacksonObjectMapper() {
            return mock(ObjectMapper.class);
        }

        @Bean
        @Qualifier("example")
        public ObjectMapper exampleObjectMapper() {
            return mock(ObjectMapper.class);
        }

    }

    @Autowired
    @Qualifier("example")
    private ObjectMapper unit;

    @Test
    void shouldOverride() {
        // TODO verify that it's actually used!
        assertThat(unit, isMock());
    }

}
