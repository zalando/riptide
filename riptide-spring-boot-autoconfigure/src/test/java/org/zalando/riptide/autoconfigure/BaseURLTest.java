package org.zalando.riptide.autoconfigure;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockWebServer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.autoconfigure.MockWebServerUtil.getRecordedRequest;
import static org.zalando.riptide.autoconfigure.MockWebServerUtil.verify;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("default")
final class BaseURLTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
            FlowAutoConfiguration.class,
            TracerAutoConfiguration.class,
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

    private final MockWebServer server = new MockWebServer();

    @Autowired
    private AtomicReference<URI> reference;

    @Autowired
    @Qualifier("example")
    private Http http;

    @SneakyThrows
    @Test
    void changesURL() {
        try {
            server.enqueue(MockWebServerUtil.emptyMockResponse());
            server.enqueue(MockWebServerUtil.emptyMockResponse());

            //path 1
            reference.set(URI.create(MockWebServerUtil.getBaseUrl(server) + "/path1"));
            http.get().call(pass()).join();
            verify(server, 1, "/path1");

            //path 2
            reference.set(URI.create(MockWebServerUtil.getBaseUrl(server) + "/path2"));
            http.get().call(pass()).join();
            var recordedRequest = getRecordedRequest(server);
            assertNotNull(recordedRequest);
            assertEquals("/path2", recordedRequest.getPath());
        } finally {
            server.shutdown();
        }
    }

}
