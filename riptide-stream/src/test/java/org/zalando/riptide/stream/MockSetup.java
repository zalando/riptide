package org.zalando.riptide.stream;

import org.springframework.http.converter.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;

import java.util.concurrent.*;

final class MockSetup {

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
