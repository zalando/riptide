package org.zalando.riptide.example.basic;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.zalando.riptide.Http;
import org.zalando.riptide.autoconfigure.HttpClientCustomizer;
import org.zalando.riptide.autoconfigure.RiptideAutoConfiguration;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.OK;

/**
 * Runnable examples demonstrating Riptide's core response routing, wired via the
 * Spring Boot starter ({@code riptide-spring-boot-starter}).
 *
 * <p>The HTTP client is configured with automatic redirect following <em>disabled</em>
 * so that 3xx responses remain visible to the routing callback. This is intentional:
 * it lets the example show how to inspect a {@code Location} header before deciding
 * what to do next.
 */
@SpringBootTest(classes = BasicRoutingExampleTest.TestConfiguration.class, webEnvironment = NONE)
final class BasicRoutingExampleTest {

    /**
     * Shared mock server for all tests. Started before the Spring context so that
     * {@link #registerBaseUrl} can supply the base-url property to the auto-configuration.
     */
    static final MockWebServer SERVER = new MockWebServer();

    @BeforeAll
    static void startServer() throws IOException {
        SERVER.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        SERVER.shutdown();
    }

    @DynamicPropertySource
    static void registerBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("riptide.clients.example.base-url",
                () -> String.format("http://%s:%d", SERVER.getHostName(), SERVER.getPort()));
    }

    /**
     * Wires the Spring Boot auto-configuration and Jackson for this test context.
     * The {@link HttpClientCustomizer} bean disables redirect following so that
     * {@code 302} responses remain observable in the routing callback.
     */
    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
    })
    static class TestConfiguration {

        @Bean
        @Qualifier("example")
        HttpClientCustomizer exampleHttpClientCustomizer() {
            return builder -> ((HttpClientBuilder) builder).disableRedirectHandling();
        }
    }

    @Autowired
    @Qualifier("example")
    private Http http;

    @Test
    void shouldCaptureLocationHeaderFromRedirectResponse() throws Exception {
        SERVER.enqueue(new MockResponse()
                .setResponseCode(FOUND.value())
                .setHeader(LOCATION, "/next"));

        BasicRoutingExample example = new BasicRoutingExample(http);
        URI location = example.followRedirect("/start");

        assertThat(location).isEqualTo(URI.create("/next"));
    }

    @Test
    void shouldDeserializeSuccessfulJsonResponse() throws Exception {
        SERVER.enqueue(new MockResponse()
                .setResponseCode(OK.value())
                .setHeader(CONTENT_TYPE, "application/json")
                .setBody("{\"name\":\"Riptide\"}"));

        BasicRoutingExample example = new BasicRoutingExample(http);
        Greeting greeting = example.fetchBody("/greet", Greeting.class);

        assertThat(greeting).isNotNull();
        assertThat(greeting.name()).isEqualTo("Riptide");
    }

    record Greeting(String name) {}
}
