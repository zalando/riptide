package org.zalando.riptide.stream;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.Http;

import java.util.concurrent.Executors;

public final class MockSetup {

    private final MockRestServiceServer server;
    private final Http http;

    public MockSetup(final String baseUrl, final Iterable<HttpMessageConverter<?>> converters) {
        final RestTemplate template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.http = Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .converters(converters)
                .baseUrl(baseUrl)
                .build();
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public Http getRest() {
        return http;
    }

}
