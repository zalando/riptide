package org.zalando.riptide.soap;

import org.junit.jupiter.api.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.zalando.riptide.*;

import javax.xml.ws.*;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.zalando.riptide.soap.SOAPRoute.*;

final class IllegalProtocolWriteTest {

    private final String address = "http://localhost:8080/hello";
    private final Endpoint endpoint = Endpoint.publish(address, new HelloService());

    private final Http unit = Http.builder()
            .executor(Executors.newSingleThreadExecutor())
            .requestFactory(new SimpleClientHttpRequestFactory())
            .baseUrl(address)
            .converter(new SOAPHttpMessageConverter("unknown protocol"))
            .converter(new SOAPFaultHttpMessageConverter())
            .build();

    @AfterEach
    void tearDown() {
        endpoint.stop();
    }

    @Test
    void shouldFailToWrite() {
        final CompletableFuture<ClientHttpResponse> future = unit.post()
                .body(new SayHello("Riptide"))
                .call(soap(SayHelloResponse.class, System.out::println));

        final CompletionException exception = assertThrows(CompletionException.class, future::join);
        assertThat(exception.getCause(), is(instanceOf(HttpMessageNotWritableException.class)));
    }

}
