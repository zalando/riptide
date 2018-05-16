package org.zalando.riptide;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

public final class FailedDispatchTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String url = "https://api.example.com";

    private final Http unit;
    private final MockRestServiceServer server;

    public FailedDispatchTest() {
        final MockSetup setup = new MockSetup();
        this.unit = setup.getHttp();
        this.server = setup.getServer();
    }

    @Test
    public void shouldThrowIfNoMatch() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("")
                        .contentType(APPLICATION_JSON));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));
        exception.expectMessage(containsString("Unable to dispatch response: 200 - OK"));
        exception.expectMessage(containsString("Content-Type=[" + APPLICATION_JSON + "]"));

        unit.options(url)
                .dispatch(contentType(),
                        // note that we don't match on application/json explicitly
                        on(SUCCESS).call(pass()),
                        on(PROBLEM).call(pass()),
                        on(ERROR).call(pass()))
                .join();
    }

    @Test
    public void shouldThrowOnFailedConversionBecauseOfUnknownContentType() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{}")
                        .contentType(MediaType.APPLICATION_ATOM_XML));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(RestClientException.class));
        exception.expectMessage("no suitable HttpMessageConverter found for response type");

        unit.get(url)
                .dispatch(status(),
                        on(OK).dispatch(series(),
                                on(SUCCESSFUL).call(Success.class, success -> {}),
                                anySeries().call(pass())),
                        on(HttpStatus.CREATED).call(pass()),
                        anyStatus().call(this::fail))
                .join();
    }

    @Test
    public void shouldThrowOnFailedConversionBecauseOfFaultyBody() {
        server.expect(requestTo(url))
                .andRespond(withSuccess()
                        .body("{")
                        .contentType(SUCCESS));

        exception.expect(CompletionException.class);

        unit.get(url)
                .dispatch(status(),
                        on(OK)
                                .dispatch(series(),
                                        on(SUCCESSFUL).call(Success.class, success -> {
                                        }),
                                        anySeries().call(pass())),
                        on(HttpStatus.CREATED).call(pass()),
                        anyStatus().call(this::fail))
                .join();
    }

    @Test
    public void shouldHandleNoBodyAtAll() {
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
    public void shouldPropagateIfNoMatch() throws Exception {
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
                        on(CLIENT_ERROR).call(pass()));

        verify(consumer).tryAccept(any());
    }

    @Test
    public void shouldPropagateMultipleLevelsIfNoMatch() throws Exception {
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
                        anySeries().call(consumer));

        verify(consumer).tryAccept(any());
    }

    @Test
    public void shouldPreserveExceptionIfPropagateFailed() {
        server.expect(requestTo(url))
                .andRespond(withCreatedEntity(URI.create("about:blank"))
                        .body(new ClassPathResource("success.json"))
                        .contentType(APPLICATION_JSON));

        exception.expect(CompletionException.class);
        exception.expectCause(instanceOf(NoRouteException.class));
        exception.expectMessage(containsString("Unable to dispatch response: 201 - Created"));
        exception.expectMessage(containsString("Content-Type=[" + APPLICATION_JSON + "]"));
        exception.expectCause(hasFeature("response", NoRouteException::getResponse, notNullValue()));
        exception.expectCause(hasFeature("raw status code", NoRouteException::getRawStatusCode, is(201)));
        exception.expectCause(hasFeature("status text", NoRouteException::getStatusText, is("Created")));
        exception.expectCause(hasFeature("response headers", NoRouteException::getResponseHeaders, is(notNullValue())));
        exception.expectCause(hasFeature("response body", NoRouteException::getResponseBody, is(notNullValue())));

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
                .join();
    }

}
