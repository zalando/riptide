package org.zalando.riptide;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.zalando.riptide.model.Success;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.concurrent.CompletionException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.PassRoute.pass;

final class ExecuteTest {

    private final String url = "https://api.example.com";

    private final Http unit;
    private final MockRestServiceServer server;

    ExecuteTest() {
        final MockSetup setup = new MockSetup();
        this.server = setup.getServer();
        this.unit = setup.getHttp();
    }

    @Test
    void shouldSendNoBody() {
        server.expect(requestTo(url))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        unit.trace(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        server.verify();
    }

    @Test
    void shouldSendHeaders() {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andExpect(header("If-Modified-Since", "Fri, 24 Nov 2017 11:02:39 GMT"))
                .andExpect(header("If-Unmodified-Since", "Fri, 17 Nov 2017 11:02:39 GMT"))
                .andExpect(header("If-Match", "A, B, C"))
                .andExpect(header("If-None-Match", "X, Y, Z"))
                .andRespond(withSuccess());

        unit.head(url)
                .header("X-Foo", "bar")
                .ifModifiedSince(OffsetDateTime.parse("2017-11-24T12:02:39+01:00"))
                .ifUnmodifiedSince(OffsetDateTime.parse("2017-11-17T12:02:39+01:00"))
                .ifMatch("A", "B", "C")
                .ifNoneMatch("X", "Y", "Z")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()))
                .join();

        server.verify();
    }

    @Test
    void shouldSendBody() {
        server.expect(requestTo(url))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        unit.post(url)
                .body(ImmutableMap.of("foo", "bar"))
                .call(pass())
                .join();

        server.verify();
    }

    @Test
    void shouldSendHeadersAndBody() {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        unit.put(url)
                .header("X-Foo", "bar")
                .body(ImmutableMap.of("foo", "bar"))
                .call(pass())
                .join();

        server.verify();
    }

    @Test
    void shouldFailIfNoConverterFoundForBody() {
        server.expect(requestTo(url))
                // actually we don't expect anything
                .andRespond(withServerError());

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.patch(url)
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_XML)
                        .body(new Success(true))
                        .dispatch(contentType())
                        .join());

        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
        assertThat(exception.getMessage(), containsString("no suitable HttpMessageConverter found"));
        assertThat(exception.getMessage(), containsString("org.zalando.riptide.model.Success"));
        assertThat(exception.getMessage(), containsString("application/xml"));
    }

    @Test
    void shouldFailIfNoConverterFoundForBodyOfUnknownContentType() {
        final MockSetup setup = new MockSetup("https://api.example.com", Collections.emptyList());
        final MockRestServiceServer server = setup.getServer();
        final Http unit = setup.getHttpBuilder()
                .converter(new Jaxb2RootElementHttpMessageConverter()).build();

        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.delete(url)
                        .body(new Success(true))
                        .dispatch(contentType())
                        .join());

        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
        assertThat(exception.getMessage(), containsString("no suitable HttpMessageConverter found"));
        assertThat(exception.getMessage(), containsString("org.zalando.riptide.model.Success"));
    }

    @Test
    void shouldFailIfNoConverterFoundForBodyOfUnsupportedContentType() {
        server.expect(requestTo(url))
                // actually we don't expect anything
                .andRespond(withServerError());

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.delete(url)
                        .contentType(MediaType.parseMediaType("application/x-json-stream"))
                        .body(new Success(true))
                        .dispatch(contentType())
                        .join());

        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
        assertThat(exception.getMessage(), containsString("no suitable HttpMessageConverter found"));
        assertThat(exception.getMessage(), containsString("org.zalando.riptide.model.Success"));
    }

}
