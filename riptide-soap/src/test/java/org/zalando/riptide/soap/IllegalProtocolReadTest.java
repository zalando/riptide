package org.zalando.riptide.soap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.zalando.riptide.Http;

import javax.xml.ws.Endpoint;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.riptide.soap.SOAPRoute.soap;

final class IllegalProtocolReadTest {

    private final String address = "http://localhost:8080/hello";
    private final Endpoint endpoint = Endpoint.publish(address, new HelloService());

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(address)
            .converter(new SOAPHttpMessageConverter())
            .converter(new SOAPFaultHttpMessageConverter("unknown protocol"))
            .build();

    @AfterEach
    void tearDown() {
        endpoint.stop();
    }

    @Test
    void shouldFailToReadFault() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new SayHello("Error"))
                .call(soap(SayHelloResponse.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(HttpMessageNotReadableException.class)));
    }

}
