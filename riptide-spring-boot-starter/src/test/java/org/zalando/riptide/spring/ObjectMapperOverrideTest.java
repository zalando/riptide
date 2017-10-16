package org.zalando.riptide.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.zalando.riptide.spring.Mocks.isMock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class ObjectMapperOverrideTest {

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
    public void shouldOverride() {
        // TODO verify that it's actually used!
        assertThat(unit, isMock());
    }

}
