package org.zalando.riptide.autoconfigure;

import dev.failsafe.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = CircuitBreakerTest.TestConfiguration.class, webEnvironment = NONE)
@Component
final class CircuitBreakerTest {

    @Configuration
    @Import(DefaultTestConfiguration.class)
    public static class TestConfiguration {

    }

    @Autowired
    private List<CircuitBreaker<ClientHttpResponse>> circuitBreakers;

    @Test
    void shouldAutowireCircuitBreakers() {
        assertThat(circuitBreakers, is(not(empty())));
    }

}
