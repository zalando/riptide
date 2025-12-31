package org.zalando.riptide.capture;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.Http;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;

final class CaptureTest {

    private final Http unit;
    private final MockRestServiceServer server;

    CaptureTest() {
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .converter(createJsonConverter())
                .converter(new StringHttpMessageConverter())
                .baseUrl("https://api.example.com")
                .build();
    }

    private JacksonJsonHttpMessageConverter createJsonConverter() {
        return new JacksonJsonHttpMessageConverter();
    }

    @Test
    void shouldCapture() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(
                withSuccess()
                        .body(new ClassPathResource("message.json"))
                        .contentType(APPLICATION_JSON));

        final Capture<JsonNode> capture = Capture.empty();

        final CompletableFuture<ClientHttpResponse> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(JsonNode.class, capture),
                        anyStatus().call(this::fail));

        final JsonNode node = future.thenApply(capture).join();

        assertThat(node.get("message").asString(), is("Hello World!"));
    }

    @Test
    void shouldCaptureNull() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(0);

        server.expect(requestTo("https://api.example.com/null"))
                .andRespond(withSuccess().headers(headers));

        final Capture<String> capture = Capture.empty();

        final CompletableFuture<ClientHttpResponse> future = unit.get("/null")
                .dispatch(status(),
                        on(OK).call(String.class, capture),
                        anyStatus().call(this::fail));

        final String body = future.thenApply(capture).join();

        assertThat(body, is(nullValue()));
    }

    @Test
    void shouldFailIfNotCaptured() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(withSuccess());

        final Capture<String> capture = Capture.empty();

        final CompletableFuture<String> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(pass()),
                        on(BAD_REQUEST).call(String.class, capture),
                        anyStatus().call(this::fail))
                .thenApply(capture);

        final CompletionException exception = assertThrows(CompletionException.class, future::join);

        assertThat(exception.getCause(), is(instanceOf(NoSuchElementException.class)));
    }

    @Test
    void shouldNotAllowSecondCaptures() {
        final Capture<String> capture = Capture.empty();
        capture.accept("foo");

        assertThrows(IllegalStateException.class, () -> capture.accept("bar"));
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getStatusCode().value());
    }

}
