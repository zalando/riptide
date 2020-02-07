package org.zalando.riptide;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.NoopRoute.noop;

final class NoopRouteTest {

    private final URI url = URI.create("https://api.example.com/blobs/123");

    private final Http unit;
    private final MockRestServiceServer server;

    NoopRouteTest() {
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .baseUrl("https://api.example.com")
                .build();
    }

    @Test
    void returnsBodyWithoutConsumingOrClosing() throws IOException {
        server.expect(requestTo(url)).andRespond(
                withSuccess()
                        .body(new byte[]{'b', 'l', 'o', 'b'})
                        .contentType(APPLICATION_OCTET_STREAM));

        final CompletableFuture<ClientHttpResponse> future =
                unit.get(url).call(noop());

        try (final InputStream body = future.join().getBody()) {
            final int ch1 = body.read();
            assertEquals('b', ch1);
        }
    }

}
