package org.zalando.riptide.capture;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.zalando.riptide.Bindings.*;
import static org.zalando.riptide.Navigators.*;
import static org.zalando.riptide.PassRoute.*;

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

    private MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        return converter;
    }

    @Test
    void shouldCapture() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(
                withSuccess()
                        .body(new ClassPathResource("message.json"))
                        .contentType(APPLICATION_JSON));

        final Capture<ObjectNode> capture = Capture.empty();

        final CompletableFuture<ClientHttpResponse> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(ObjectNode.class, capture),
                        anyStatus().call(this::fail));

        final ObjectNode node = future.thenApply(capture).join();

        assertThat(node.get("message").asText(), is("Hello World!"));
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
        throw new AssertionError(response.getRawStatusCode());
    }

}
