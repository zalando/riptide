package org.zalando.riptide.autoconfigure;

import lombok.SneakyThrows;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.junit.jupiter.api.AfterEach;
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
import org.zalando.opentracing.flowid.autoconfigure.OpenTracingFlowIdAutoConfiguration;
import org.zalando.riptide.Http;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.autoconfigure.MockWebServerUtil.getBaseUrl;
import static org.zalando.riptide.autoconfigure.MockWebServerUtil.textMockResponse;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("caching")
final class CachingTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            OpenTracingFlowIdAutoConfiguration.class,
            MetricsTestAutoConfiguration.class,
    })
    public static class TestConfiguration {

        @Bean
        public HttpCacheStorage publicHttpCacheStorage() {
            return new BasicHttpCacheStorage(CacheConfig.DEFAULT);
        }

    }

    private final MockWebServer server = new MockWebServer();

    @Autowired
    @Qualifier("public")
    private Http shared;

    @Autowired
    @Qualifier("private")
    private Http nonShared;

    @Autowired
    @Qualifier("heuristic")
    private Http heuristic;

    @SneakyThrows
    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    @Test
    void shouldCacheInSharedCacheMode() {
        server.enqueue(new MockResponse()
                .setBody("Hello")
                .setHeader("Content-Type", "text/plain")
                .setHeader("Cache-Control", "max-age=300, s-maxage=300")
        );

        shared.get(getBaseUrl(server)).call(pass()).join();
        shared.get(getBaseUrl(server)).call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldNotCacheWithAuthorizationInSharedCacheMode() {
        server.enqueue(textMockResponse("Hello")
                .setHeader("Cache-Control", "max-age=300")
        );
        server.enqueue(textMockResponse("Hello")
                .setHeader("Cache-Control", "max-age=300")
        );
        shared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        MockWebServerUtil.verify(server, 2, "/");
    }

    @Test
    void shouldCacheWithAuthorizationInSharedCacheModeWithPublicDirective() {
        server.enqueue(textMockResponse("Hello")
                .setHeader("Cache-Control", "public, s-maxage=300")
        );

        shared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldCacheInNonSharedCacheMode() {
        server.enqueue(textMockResponse("Hello")
                .setHeader("Cache-Control", "max-age=300")
        );

        nonShared.get(getBaseUrl(server)).call(pass()).join();
        nonShared.get(getBaseUrl(server)).call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldCacheWithAuthorizationInNonSharedCacheMode() {
        server.enqueue(textMockResponse("Hello")
                .setHeader("Cache-Control", "max-age=300")
        );

        nonShared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        nonShared.get(getBaseUrl(server))
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldCacheWithHeuristic() {
        server.enqueue(textMockResponse("Hello"));

        heuristic.get(getBaseUrl(server)).call(pass()).join();
        heuristic.get(getBaseUrl(server)).call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

    @Test
    void shouldCacheWithAuthorizationAndHeuristic() {
        server.enqueue(textMockResponse("Hello"));

        heuristic.get(getBaseUrl(server))
                .header("Authorization", "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.")
                .call(pass()).join();

        heuristic.get(getBaseUrl(server))
                .header("Authorization", "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.")
                .call(pass()).join();

        MockWebServerUtil.verify(server, 1, "/");
    }

}
