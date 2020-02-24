package org.zalando.riptide.autoconfigure;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.tracer.autoconfigure.TracerAutoConfiguration;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.PassRoute.pass;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("default")
final class BaseURLTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
            OpenTracingTestAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    public static class TestConfiguration {

        @Bean
        public AtomicReference<URI> reference() {
            return new AtomicReference<>();
        }

        @Bean
        public BaseURL exampleBaseURL(final AtomicReference<URI> reference) {
            return reference::get;
        }

    }

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    @Autowired
    private AtomicReference<URI> reference;

    @Autowired
    @Qualifier("example")
    private Http http;

    @Test
    void changesURL() {
        driver.addExpectation(
                onRequestTo("http://www.example.org/"),
                giveEmptyResponse());

        reference.set(URI.create("http://www.example.org/"));
        http.get().call(pass()).join();

        driver.addExpectation(
                onRequestTo("http://www.example.de/"),
                giveEmptyResponse());

        reference.set(URI.create("http://www.example.de/"));
        http.get().call(pass()).join();
    }

}
