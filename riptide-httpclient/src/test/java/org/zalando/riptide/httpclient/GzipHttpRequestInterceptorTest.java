package org.zalando.riptide.httpclient;

import com.github.restdriver.clientdriver.ClientDriverRule;
import lombok.Value;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.zalando.riptide.Http;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.github.restdriver.clientdriver.ClientDriverRequest.Method.POST;
import static com.github.restdriver.clientdriver.RestClientDriver.giveEmptyResponse;
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.zalando.riptide.PassRoute.pass;

public final class GzipHttpRequestInterceptorTest {

    @Rule
    public final ClientDriverRule driver = new ClientDriverRule();

    @Value
    private static final class NonTextHttpRequestInterceptor implements HttpRequestInterceptor {
        HttpRequestInterceptor interceptor;

        @Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            @Nullable final Header contentType = request.getFirstHeader("Content-Type");

            if (contentType == null || !"text/plain".equals(contentType.getValue())) {
                interceptor.process(request, context);
            }
        }
    }

    private final CloseableHttpClient client = HttpClientBuilder.create()
            .addInterceptorLast(new NonTextHttpRequestInterceptor(new GzipHttpRequestInterceptor()))
            .build();

    private final AsyncListenableTaskExecutor executor = new ConcurrentTaskExecutor();
    private final RestAsyncClientHttpRequestFactory factory = new RestAsyncClientHttpRequestFactory(client, executor);

    private final Http http = Http.builder()
            .baseUrl(driver.getBaseUrl())
            .requestFactory(factory)
            .build();

    @After
    public void closeClient() throws IOException {
        client.close();
    }

    @Test
    public void shouldSkipRequestWithoutBody() {
        driver.addExpectation(onRequestTo("/")
                        .withoutHeader("Content-Type")
                        .withoutHeader("Content-Length")
                        .withoutHeader("Content-Encoding")
                        .withoutHeader("Transfer-Encoding"),
                giveEmptyResponse());

        http.get("/").call(pass()).join();
    }

    @Test
    public void shouldCompressBody() {
        driver.addExpectation(onRequestTo("/").withMethod(POST)
                        .withHeader("Content-Type", "application/json")
                        .withoutHeader("Content-Length")
                        .withHeader("Content-Encoding", "gzip")
                        .withHeader("Transfer-Encoding", "chunked")
                        .withBody(not("{}"), "application/json"),
                giveEmptyResponse());

        http.post("/")
                .contentType(APPLICATION_JSON)
                .body(emptyMap())
                .call(pass())
                .join();
    }

    @Test
    public void shouldNotCompressBody() {
        driver.addExpectation(onRequestTo("/").withMethod(POST)
                        .withHeader("Content-Type", "text/plain")
                        .withHeader("Content-Length", any(String.class))
                        .withoutHeader("Content-Encoding")
                        .withoutHeader("Transfer-Encoding")
                        .withBody("Hello, world!", "text/plain"),
                giveEmptyResponse());

        http.post("/")
                .contentType(TEXT_PLAIN)
                .body("Hello, world!")
                .call(pass())
                .join();
    }

    @Test
    public void shouldNotCompressAbsentBody() {
        driver.addExpectation(onRequestTo("/").withMethod(POST)
                        .withoutHeader("Content-Type")
                        .withHeader("Content-Length", "0")
                        .withoutHeader("Content-Encoding")
                        .withoutHeader("Transfer-Encoding"),
                giveEmptyResponse());

        http.post("/").call(pass()).join();
    }

}
