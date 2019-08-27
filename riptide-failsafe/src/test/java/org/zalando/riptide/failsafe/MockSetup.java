package org.zalando.riptide.failsafe;

import com.fasterxml.jackson.databind.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;
import org.zalando.riptide.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.MoreObjects.*;

final class MockSetup {

    private static final List<HttpMessageConverter<?>> DEFAULT_CONVERTERS =
            Arrays.asList(new StringHttpMessageConverter(),
                    createJsonConverter());

    private static MappingJackson2HttpMessageConverter createJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        return converter;
    }


    private final String baseUrl;
    private final Iterable<HttpMessageConverter<?>> converters;
    private final RestTemplate template;
    private final MockRestServiceServer server;

    public MockSetup() {
        this("https://api.example.com", null);
    }

    public MockSetup(final String baseUrl) {
        this(baseUrl, null);
    }

    private MockSetup(@Nullable final String baseUrl, @Nullable final Iterable<HttpMessageConverter<?>> converters) {
        this.baseUrl = baseUrl;
        this.converters = converters;
        this.template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    private Http.ConfigurationStage getRestBuilder() {
        return Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .converters(firstNonNull(converters, DEFAULT_CONVERTERS))
                .baseUrl(baseUrl);
    }
    public Http getRest() {
        return getRestBuilder().build();
    }

}
