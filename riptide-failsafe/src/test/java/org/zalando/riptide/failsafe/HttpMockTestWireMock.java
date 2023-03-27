package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.RequestMatcher;
import dev.failsafe.CircuitBreaker;
import dev.failsafe.RetryPolicy;
import lombok.SneakyThrows;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.idempotency.IdempotencyPredicate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static com.google.common.io.Resources.getResource;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Attributes.RETRIES;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.Route.call;
import static org.zalando.riptide.failsafe.CheckedPredicateConverter.toCheckedPredicate;
import static org.zalando.riptide.failsafe.RetryRoute.retry;
import static org.zalando.riptide.faults.Predicates.alwaysTrue;
import static org.zalando.riptide.faults.TransientFaults.transientConnectionFaults;
import static org.zalando.riptide.faults.TransientFaults.transientSocketFaults;

@WireMockTest
final class HttpMockTestWireMock {

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                    .setSocketTimeout(500)
                    .build())
            .build();

    private final AtomicInteger attempt = new AtomicInteger();

    private Http buildHttp(WireMockRuntimeInfo wmRuntimeInfo) {
        return Http.builder()
                .executor(newFixedThreadPool(2)) // to allow for nested calls
                .requestFactory(new ApacheClientHttpRequestFactory(client))
                .baseUrl(wmRuntimeInfo.getHttpBaseUrl())
                .converter(createJsonConverter())
                .plugin(new Plugin() {
                    @Override
                    public RequestExecution aroundNetwork(final RequestExecution execution) {
                        return arguments -> {
                            arguments.getAttribute(RETRIES).ifPresent(attempt::set);
                            return execution.execute(arguments);
                        };
                    }
                })
                .plugin(new FailsafePlugin()
                        .withPolicy(new RetryRequestPolicy(
                                RetryPolicy.<ClientHttpResponse>builder()
                                        .handleIf(toCheckedPredicate(transientSocketFaults()))
                                        .handle(RetryException.class)
                                        .handleResultIf(this::isBadGateway)
                                        .withDelay(Duration.ofMillis(500))
                                        .withMaxRetries(4)
                                        .build())
                                .withPredicate(new IdempotencyPredicate()))
                        .withPolicy(new RetryRequestPolicy(
                                RetryPolicy.<ClientHttpResponse>builder()
                                        .handleIf(toCheckedPredicate(transientConnectionFaults()))
                                        .withDelay(Duration.ofMillis(500))
                                        .withMaxRetries(4)
                                        .build())
                                .withPredicate(alwaysTrue()))
                        .withPolicy(CircuitBreaker.<ClientHttpResponse>builder()
                                .withFailureThreshold(5, 10)
                                .withSuccessThreshold(5)
                                .withDelay(Duration.ofMinutes(1))
                                .build()))
                .build();
    }

    // workaround to process differently several invocations of the same request
    private final class RequestNumberMatcher extends RequestMatcher {

        private AtomicInteger sharedRetriesCount;
        private int targetNumber;

        public RequestNumberMatcher(AtomicInteger sharedRetriesCount, int targetNumber) {
            this.sharedRetriesCount = sharedRetriesCount;
            this.targetNumber = targetNumber;
        }

        @Override
        public String getName() {
            return "request_" + targetNumber;
        }

        @Override
        public MatchResult match(Request value) {
            return sharedRetriesCount.compareAndSet(targetNumber - 1, targetNumber) ? MatchResult.exactMatch() : MatchResult.noMatch();
        }
    }

    @SneakyThrows
    private boolean isBadGateway(@Nullable final ClientHttpResponse response) {
        return response != null && response.getStatusCode() == HttpStatus.BAD_GATEWAY;
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }

    @SneakyThrows
    @Test
    void shouldRetrySuccessfully(WireMockRuntimeInfo wmRuntimeInfo) {
        var unit = buildHttp(wmRuntimeInfo);
        AtomicInteger sharedRetriesCount = new AtomicInteger();
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(get("/foo")
                .andMatching(new RequestNumberMatcher(sharedRetriesCount, 1))
                .willReturn(status(NO_CONTENT.value())
                        .withFixedDelay(800))
        );

        wireMock.register(get("/foo")
                .andMatching(new RequestNumberMatcher(sharedRetriesCount, 2))
                .willReturn(ok()
                        .withBody(getResource("contributors.json").openStream().readAllBytes())
                        .withHeader("Content-Type", "application/json"))
        );


        unit.get("/foo")
                .call(pass())
                .join();

        assertThat(wireMock.findAllUnmatchedRequests(), empty());
        assertEquals(2, wireMock.find(newRequestPattern().withUrl("/foo")).size());
    }


    @Test
    void shouldRetryUnsuccessfully(WireMockRuntimeInfo wmRuntimeInfo) {
        var unit = buildHttp(wmRuntimeInfo);
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(get("/bar")
                .willReturn(status(NO_CONTENT.value()).withFixedDelay(800)));

        final CompletionException exception = assertThrows(CompletionException.class,
                unit.get("/bar").call(pass())::join);

        assertThat(exception.getCause(), is(instanceOf(SocketTimeoutException.class)));
        assertThat(wireMock.findAllUnmatchedRequests(), empty());
        assertEquals(5, wireMock.find(newRequestPattern().withUrl("/bar")).size());
    }

    @Test
    void shouldRetryExplicitly(WireMockRuntimeInfo wmRuntimeInfo) {
        var unit = buildHttp(wmRuntimeInfo);
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        AtomicInteger sharedRetriesCount = new AtomicInteger();

        wireMock.register(get("/baz")
                .andMatching(new RequestNumberMatcher(sharedRetriesCount, 1))
                .willReturn(status(503)
                        .withHeader("X-RateLimit-Reset", "1523486068")
        ));

        wireMock.register(get("/baz")
                .andMatching(new RequestNumberMatcher(sharedRetriesCount, 2))
                .willReturn(status(NO_CONTENT.value()))
        );

        unit.get("/baz")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()),
                        anySeries().dispatch(org.zalando.riptide.Navigators.status(),
                                on(SERVICE_UNAVAILABLE).call(retry())))
                .join();

        assertThat(wireMock.findAllUnmatchedRequests(), empty());
        assertEquals(2, wireMock.find(newRequestPattern().withUrl("/baz")).size());
    }

    @Test
    void shouldAllowNestedCalls(WireMockRuntimeInfo wmRuntimeInfo) {
        var unit = buildHttp(wmRuntimeInfo);
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        wireMock.register(get("/foo").willReturn(status(NO_CONTENT.value())));
        wireMock.register(get("/bar").willReturn(status(NO_CONTENT.value())));

        assertTimeout(Duration.ofSeconds(1),
                unit.get("/foo")
                        .call(call(() -> unit.get("/bar").call(pass()).join()))::join);

        assertThat(wireMock.findAllUnmatchedRequests(), empty());
        assertEquals(1, wireMock.find(newRequestPattern().withUrl("/foo")).size());
        assertEquals(1, wireMock.find(newRequestPattern().withUrl("/bar")).size());
    }


    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(createObjectMapper());
        return converter;
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
