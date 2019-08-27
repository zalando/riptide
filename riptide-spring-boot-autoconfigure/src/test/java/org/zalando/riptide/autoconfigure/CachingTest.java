package org.zalando.riptide.autoconfigure;

import com.github.restdriver.clientdriver.*;
import org.apache.http.client.cache.*;
import org.apache.http.impl.client.cache.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jackson.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.zalando.logbook.autoconfigure.*;
import org.zalando.riptide.*;
import org.zalando.tracer.autoconfigure.*;

import static com.github.restdriver.clientdriver.RestClientDriver.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;
import static org.zalando.riptide.PassRoute.*;

@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("caching")
final class CachingTest {

    @Configuration
    @ImportAutoConfiguration({
            RiptideAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            LogbookAutoConfiguration.class,
            TracerAutoConfiguration.class,
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
