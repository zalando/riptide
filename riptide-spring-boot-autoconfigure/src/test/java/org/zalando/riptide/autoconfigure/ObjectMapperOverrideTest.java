package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.autoconfigure.Mocks.isMock;

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
