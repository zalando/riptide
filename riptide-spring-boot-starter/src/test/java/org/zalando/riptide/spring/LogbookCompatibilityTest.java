package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.logbook.spring.LogbookAutoConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public final class LogbookCompatibilityTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    @ImportAutoConfiguration({
            LogbookAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    public static class TestConfiguration {

    }

    @Test
    public void shouldUseInterceptors() {
        // TODO implement
    }

}
