package org.zalando.riptide.autoconfigure;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
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

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.PassRoute.pass;

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

    private final ClientDriver driver = new ClientDriverFactory().createClientDriver();

    @Autowired
    @Qualifier("public")
    private Http shared;

    @Autowired
    @Qualifier("private")
    private Http nonShared;

    @Autowired
    @Qualifier("heuristic")
    private Http heuristic;

    @AfterEach
    void verify() {
        driver.verify();
    }

    @Test
    void shouldCacheInSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "max-age=300, s-maxage=300"));

        shared.get(driver.getBaseUrl()).call(pass()).join();
        shared.get(driver.getBaseUrl()).call(pass()).join();
    }

    @Test
    void shouldNotCacheWithAuthorizationInSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "max-age=300"));
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "max-age=300"));

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    void shouldCacheWithAuthorizationInSharedCacheModeWithPublicDirective() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "public, s-maxage=300"));

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    void shouldCacheInNonSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "max-age=300"));

        nonShared.get(driver.getBaseUrl()).call(pass()).join();
        nonShared.get(driver.getBaseUrl()).call(pass()).join();
    }

    @Test
    void shouldCacheWithAuthorizationInNonSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain").withHeader("Cache-Control", "max-age=300"));

        nonShared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        nonShared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    void shouldCacheWithHeuristic() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain"));

        heuristic.get(driver.getBaseUrl()).call(pass()).join();
        heuristic.get(driver.getBaseUrl()).call(pass()).join();
    }

    @Test
    void shouldCacheWithAuthorizationAndHeuristic() {
        driver.addExpectation(onRequestTo("/"),
                giveResponse("Hello", "text/plain"));

        heuristic.get(driver.getBaseUrl())
                .header("Authorization", "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.")
                .call(pass()).join();

        heuristic.get(driver.getBaseUrl())
                .header("Authorization", "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.e30.")
                .call(pass()).join();
    }

}
