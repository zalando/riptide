package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.zalando.logbook.spring.LogbookAutoConfiguration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
final class LogbookCompatibilityTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    @ImportAutoConfiguration({
            LogbookAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    public static class TestConfiguration {

    }

    @Test
    void shouldUseInterceptors() {
        // TODO implement
    }

}
