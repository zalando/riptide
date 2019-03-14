package org.zalando.riptide.soap;

import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.client.RestClientException;
import org.zalando.riptide.Http;
import org.zalando.riptide.capture.Capture;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;

import javax.xml.soap.SOAPFault;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.TEXT_XML;
import static org.zalando.riptide.PassRoute.pass;
import static org.zalando.riptide.soap.SOAPRoute.soap;

final class SOAPTest {

    private final String address = "http://localhost:8080/hello";
    private final Endpoint endpoint = Endpoint.publish(address, new HelloService());

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new ApacheClientHttpRequestFactory(HttpClients.createDefault()))
            .baseUrl(address)
            .converter(new SOAPHttpMessageConverter())
            .converter(new SOAPFaultHttpMessageConverter())
            .build();

    @AfterEach
    void tearDown() {
        endpoint.stop();
    }

    @Test
    void shouldSendAndReceive() {
        final Capture<SayHelloResponse> capture = Capture.empty();

        final SayHelloResponse response = unit.post()
                .body(new SayHello("Riptide"))
                .call(soap(SayHelloResponse.class, capture))
                .thenApply(capture)
                .join();

        assertEquals("Hello, Riptide.", response.getReturn());
    }

    @Test
    void shouldSendAndReceiveFault() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new SayHello("Error"))
                .call(soap(SayHelloResponse.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        final SOAPFaultException cause = (SOAPFaultException) exception.getCause();
        final SOAPFault fault = cause.getFault();

        assertEquals("Error is not supported", fault.getFaultString());
    }

    @Test
    void shouldFailToWrite() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new InvalidSayHello(singleton("Riptide")))
                .call(pass());

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(HttpMessageNotWritableException.class)));
    }

    @Test
    void shouldFailToWriteIncompatibleMediaType() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .contentType(MediaType.APPLICATION_XML)
                .body(new SayHello("Riptide"))
                .call(soap(SayHelloResponse.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
    }

    @Test
    void shouldFailToWriteFault() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(mock(SOAPFault.class))
                .call(pass());

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
    }

    @Test
    void shouldFailToWriteUnsupportedType() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .contentType(TEXT_XML)
                .body(singleton("Riptide"))
                .call(pass());

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
    }

    @Test
    void shouldFailToRead() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new SayHello("Riptide"))
                .call(soap(InvalidSayHelloResponse.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(HttpMessageNotReadableException.class)));
    }

    @Test
    void shouldFailToReadUnsupportedType() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new SayHello("Riptide"))
                .call(soap(String.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(RestClientException.class)));
    }

}
