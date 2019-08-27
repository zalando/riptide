package org.zalando.riptide.soap;

import org.apache.http.impl.client.*;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.capture.*;
import org.zalando.riptide.httpclient.*;

import javax.xml.soap.*;
import javax.xml.ws.*;
import javax.xml.ws.soap.*;
import java.util.concurrent.*;

import static java.util.Collections.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;
import static org.zalando.riptide.PassRoute.*;
import static org.zalando.riptide.soap.SOAPRoute.*;

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
