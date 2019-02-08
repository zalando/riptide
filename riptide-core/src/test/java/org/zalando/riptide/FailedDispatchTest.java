package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.zalando.riptide.model.Success;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hobsoft.hamcrest.compose.ComposeMatchers.hasFeature;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.MOVED_PERMANENTLY;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.anyContentType;
import static org.zalando.riptide.Bindings.anySeries;
import static org.zalando.riptide.Bindings.anyStatus;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.contentType;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Navigators.status;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.model.MediaTypes.ERROR;
import static org.zalando.riptide.model.MediaTypes.PROBLEM;
import static org.zalando.riptide.model.MediaTypes.SUCCESS;

final class FailedDispatchTest {

    private final String url = "https://api.example.com";

    private final Http unit;
    private final MockRestServiceServer server;

    FailedDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    void shouldThrowIfNoMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("")
                        .contentType(APPLICATION_JSON));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.options(url)
                .dispatch(contentType(),
                        // note that we don't match on application/json explicitly
                        on(SUCCESS).call(pass()),
                        on(PROBLEM).call(pass()),
                        on(ERROR).call(pass()))
                .join());

        assertThat(exception.getCause(), is(instanceOf(UnexpectedResponseException.class)));
        assertThat(exception.getMessage(), containsString("Unable to dispatch response: 200 - OK"));
        assertThat(exception.getMessage(), containsString("Content-Type"));
        assertThat(exception.getMessage(), containsString(APPLICATION_JSON_VALUE));
    }

    @Test
    void shouldThrowOnFailedConversionBecauseOfUnknownContentType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{}")
                        .contentType(MediaType.APPLICATION_ATOM_XML));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.get(url)
                .dispatch(status(),
                        on(OK).dispatch(series(),
                                on(SUCCESSFUL).call(Success.class, success -> {}),
                                anySeries().call(pass())),
                        on(HttpStatus.CREATED).call(pass()),
                        anyStatus().call(this::fail))
                .join());

        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
        assertThat(exception.getMessage(), containsString("no suitable HttpMessageConverter found for response type"));
    }

    @Test
    void shouldThrowOnFailedConversionBecauseOfFaultyBody() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{")
                        .contentType(SUCCESS));

        assertThrows(CompletionException.class, () ->
                unit.get(url)
                .dispatch(status(),
                        on(OK)
                                .dispatch(series(),
                                        on(SUCCESSFUL).call(Success.class, success -> {
                                        }),
                                        anySeries().call(pass())),
                        on(HttpStatus.CREATED).call(pass()),
                        anyStatus().call(this::fail))
                .join());
    }

    @Test
    void shouldHandleNoBodyAtAll() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(0);

        server.expect(requestTo(url))
                .andRespond(withStatus(OK)
                        .headers(headers)
                        .contentType(SUCCESS));

        final AtomicReference<Success> success = new AtomicReference<>();

        unit.get(url)
                .dispatch(status(),
                        on(OK)
                                .dispatch(contentType(),
                                        on(SUCCESS).call(Success.class, success::set),
                                        anyContentType().call(this::fail)),
                        on(HttpStatus.CREATED).call(Success.class, success::set),
                        anyStatus().call(this::fail))
                .join();

        assertThat(success.get(), is(nullValue()));
    }

    private void fail(final ClientHttpResponse response) {
        throw new AssertionError("Should not have been executed, but received: " + response);
    }

    @Test
    void shouldPropagateIfNoMatch() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(status(),
                                on(OK).dispatch(contentType(),
                                        on(APPLICATION_XML).call(pass()),
                                        on(TEXT_PLAIN).call(pass())),
                                on(ACCEPTED).call(pass()),
                                anyStatus().call(consumer)),
                        on(CLIENT_ERROR).call(pass()))
                .join();

        verify(consumer).tryAccept(any());
    }

    @Test
    void shouldPropagateMultipleLevelsIfNoMatch() throws Exception {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final ClientHttpResponseConsumer consumer = mock(ClientHttpResponseConsumer.class);

        unit.get(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(status(),
                                on(OK).dispatch(contentType(),
                                        on(APPLICATION_XML).call(pass()),
                                        on(TEXT_PLAIN).call(pass())),
                                on(ACCEPTED).call(pass())),
                        on(CLIENT_ERROR).call(pass()),
                        anySeries().call(consumer))
                .join();

        verify(consumer).tryAccept(any());
    }



    @Test
    void shouldPreserveExceptionIfPropagateFailed() {
        server.expect(requestTo(url))
                .andRespond(withCreatedEntity(URI.create("about:blank"))
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        final CompletionException exception = assertThrows(CompletionException.class, () ->
                unit.post(url)
                .dispatch(series(),
                        on(SUCCESSFUL).dispatch(contentType(),
                                on(APPLICATION_JSON).dispatch(status(),
                                        on(OK).call(pass()),
                                        on(MOVED_PERMANENTLY).call(pass()),
                                        on(NOT_FOUND).call(pass())),
                                on(APPLICATION_XML).call(pass()),
                                on(TEXT_PLAIN).call(pass())),
                        on(CLIENT_ERROR).call(pass()))
                .join());

        assertThat(exception.getMessage(), containsString("Unable to dispatch response: 201 - Created"));
        assertThat(exception.getMessage(), containsString("Content-Type"));
        assertThat(exception.getMessage(), containsString(APPLICATION_JSON_VALUE));

        final UnexpectedResponseException cause = (UnexpectedResponseException) exception.getCause();
        assertThat(cause, hasFeature("raw status code", UnexpectedResponseException::getRawStatusCode, is(201)));
        assertThat(cause, hasFeature("status text", UnexpectedResponseException::getStatusText, is("Created")));
        assertThat(cause, hasFeature("response headers", UnexpectedResponseException::getResponseHeaders, is(notNullValue())));
        assertThat(cause, hasFeature("response body", UnexpectedResponseException::getResponseBody, is(notNullValue())));
    }

}
