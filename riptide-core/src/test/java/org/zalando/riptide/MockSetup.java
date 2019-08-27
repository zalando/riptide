package org.zalando.riptide;

import com.fasterxml.jackson.databind.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.test.web.client.*;
import org.springframework.web.client.*;

import javax.annotation.*;
import java.util.*;
import java.util.concurrent.*;

import static com.google.common.base.MoreObjects.*;

final class MockSetup {

    private static final List<HttpMessageConverter<?>> DEFAULT_CONVERTERS =
            Arrays.asList(new StringHttpMessageConverter(), defaultJsonConverter());

    private final String baseUrl;
    private final Iterable<HttpMessageConverter<?>> converters;
    private final RestTemplate template;
    private final MockRestServiceServer server;

    private static MappingJackson2HttpMessageConverter defaultJsonConverter() {
        final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(new ObjectMapper().findAndRegisterModules());
        return converter;
    }

    public MockSetup() {
        this("https://api.example.com", null);
    }

    public MockSetup(@Nullable final String baseUrl) {
        this(baseUrl, null);
    }

    public MockSetup(@Nullable final String baseUrl, @Nullable final Iterable<HttpMessageConverter<?>> converters) {
        this.baseUrl = baseUrl;
        this.converters = converters;
        this.template = new RestTemplate();
        this.server = MockRestServiceServer.createServer(template);
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public Http.ConfigurationStage getHttpBuilder() {
        return Http.builder()
                .executor(Executors.newSingleThreadExecutor())
                .requestFactory(template.getRequestFactory())
                .converters(firstNonNull(converters, DEFAULT_CONVERTERS))
                .baseUrl(baseUrl)
                .defaultPlugins();
    }
    public Http getHttp() {
        return getHttpBuilder().build();
    }

}
