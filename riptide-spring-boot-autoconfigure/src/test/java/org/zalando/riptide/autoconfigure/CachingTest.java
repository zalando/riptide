package org.zalando.riptide.autoconfigure;

import com.github.restdriver.clientdriver.ClientDriverRule;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.impl.client.cache.BasicHttpCacheStorage;
import org.apache.http.impl.client.cache.CacheConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.zalando.logbook.spring.LogbookAutoConfiguration;
import org.zalando.riptide.Http;
import org.zalando.tracer.spring.TracerAutoConfiguration;

import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;
import static org.zalando.riptide.PassRoute.pass;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = NONE)
@ActiveProfiles("caching")
public class CachingTest {

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
        public HttpCacheStorage sharedHttpCacheStorage() {
            return new BasicHttpCacheStorage(CacheConfig.DEFAULT);
        }

    }

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Autowired
    @Qualifier("public")
    private Http shared;

    @Autowired
    @Qualifier("private")
    private Http nonShared;

    @Autowired
    @Qualifier("heuristic")
    private Http heuristic;

    @Test
    public void shouldCacheInSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "max-age=300, s-maxage=300"));

        shared.get(driver.getBaseUrl()).call(pass()).join();
        shared.get(driver.getBaseUrl()).call(pass()).join();
    }

    @Test
    public void shouldNotCacheWithAuthorizationInSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "max-age=300"));
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "max-age=300"));

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    public void shouldCacheWithAuthorizationInSharedCacheModeWithPublicDirective() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "public, s-maxage=300"));

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        shared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    public void shouldCacheInNonSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "max-age=300"));

        nonShared.get(driver.getBaseUrl()).call(pass()).join();
        nonShared.get(driver.getBaseUrl()).call(pass()).join();
    }

    @Test
    public void shouldCacheWithAuthorizationInNonSharedCacheMode() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "max-age=300"));

        nonShared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();

        nonShared.get(driver.getBaseUrl())
                .header("Authorization", "Bearer XYZ")
                .call(pass()).join();
    }

    @Test
    public void shouldCacheWithHeuristic() {
        driver.addExpectation(onRequestTo("/"),
                giveEmptyResponse().withHeader("Cache-Control", "public"));

        heuristic.get(driver.getBaseUrl()).call(pass()).join();
        heuristic.get(driver.getBaseUrl()).call(pass()).join();
    }

}
