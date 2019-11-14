package org.zalando.riptide.compression;

import com.github.restdriver.clientdriver.ClientDriver;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory.Mode;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.zalando.riptide.PassRoute.pass;

class RequestCompressionPluginTest {

    private final ClientDriver driver = GzipClientDriver.create();
    private final ExecutorService executor = newSingleThreadExecutor();

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        driver.verify();
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldCompressRequestUsingFactory(final ClientHttpRequestFactory factory) {
        driver.addExpectation(onRequestTo("/")
                        .withMethod(POST)
                        .withHeader("X-Content-Encoding", "gzip") // written by Jetty's GzipHandler
                        .withBody(equalTo("{}"), "application/json"),
                giveResponse("", "text/plain"));

        final Http http = buildHttp(factory, new RequestCompressionPlugin());
        http.post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .call(pass())
                .join();
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldNotCompressEmptyBody(final ClientHttpRequestFactory factory) {
        driver.addExpectation(onRequestTo("/")
                        .withBody(emptyString(), "text/plain")
                        .withoutHeader("Content-Encoding")
                        .withoutHeader("X-Content-Encoding"),
                giveResponse("", "text/plain"));

        final Http http = buildHttp(factory, new RequestCompressionPlugin());
        http.get("/")
                .contentType(MediaType.TEXT_PLAIN)
                .call(pass())
                .join();
    }

    @ParameterizedTest
    @ArgumentsSource(RequestFactorySource.class)
    void shouldCompressWithGivenAlgorithm(final ClientHttpRequestFactory factory) {
        driver.addExpectation(onRequestTo("/")
                        .withMethod(POST)
                        .withHeader("Content-Encoding", "identity") // not handled by Jetty
                        .withoutHeader("X-Content-Encoding")
                        .withBody(equalTo("{}"), "application/json"),
                giveResponse("", "text/plain"));

        final Http http = buildHttp(factory, new RequestCompressionPlugin(Compression.of("identity", it -> it)));
        http.post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .call(pass())
                .join();
    }

    private Http buildHttp(final ClientHttpRequestFactory factory, final Plugin... plugins) {
        return Http.builder()
                .executor(executor)
                .requestFactory(factory)
                .baseUrl(driver.getBaseUrl())
                .plugins(asList(plugins))
                .build();
    }

    static class RequestFactorySource implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    new SimpleClientHttpRequestFactory(),
                    new Netty4ClientHttpRequestFactory(),
                    new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()),
                    new ApacheClientHttpRequestFactory(HttpClients.createDefault(), Mode.BUFFERING),
                    new ApacheClientHttpRequestFactory(HttpClients.createDefault(), Mode.STREAMING)
            ).map(Arguments::of);
        }
    }

}
