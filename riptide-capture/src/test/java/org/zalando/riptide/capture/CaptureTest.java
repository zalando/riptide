package org.zalando.riptide.capture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.Http;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;

public final class CaptureTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final Http unit;
    private final MockRestServiceServer server;

    public CaptureTest() {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Http.builder()
                .requestFactory(template.getAsyncRequestFactory())
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
    public void shouldCapture() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(
                withSuccess()
                        .body(new ClassPathResource("message.json"))
                        .contentType(APPLICATION_JSON));

        final Capture<ObjectNode> capture = Capture.empty();

        final CompletableFuture<Void> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(ObjectNode.class, capture),
                        anyStatus().call(this::fail));

        final ObjectNode node = future.thenApply(capture).join();

        assertThat(node.get("message").asText(), is("Hello World!"));
    }

    @Test
    public void shouldCaptureNull() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(0);

        server.expect(requestTo("https://api.example.com/null"))
                .andRespond(withSuccess().headers(headers));

        final Capture<String> capture = Capture.empty();

        final CompletableFuture<Void> future = unit.get("/null")
                .dispatch(status(),
                        on(OK).call(String.class, capture),
                        anyStatus().call(this::fail));

        final String body = future.thenApply(capture).join();

        assertThat(body, is(nullValue()));
    }

    @Test
    public void shouldFailIfNotCaptured() {
        server.expect(requestTo("https://api.example.com/accounts/123")).andRespond(withSuccess());

        final Capture<String> capture = Capture.empty();

        final CompletableFuture<String> future = unit.get("/accounts/123")
                .dispatch(status(),
                        on(OK).call(pass()),
                        on(BAD_REQUEST).call(String.class, capture),
                        anyStatus().call(this::fail))
                .thenApply(capture);

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(NoSuchElementException.class));

        future.join();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowSecondCaptures() {
        final Capture<String> capture = Capture.empty();
        capture.accept("foo");
        capture.accept("bar");
    }

    private void fail(final ClientHttpResponse response) throws IOException {
        throw new AssertionError(response.getRawStatusCode());
    }

}
