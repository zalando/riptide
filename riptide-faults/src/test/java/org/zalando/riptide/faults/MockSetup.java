package org.zalando.riptide.faults;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.zalando.riptide.Http;
import org.zalando.riptide.HttpBuilder;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.firstNonNull;

public final class MockSetup {

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
    private final AsyncRestTemplate template;
    private final MockRestServiceServer server;

    public MockSetup() {
        this("https://api.example.com", null);
    }

    public MockSetup(final String baseUrl) {
        this(baseUrl, null);
    }

    public MockSetup(@Nullable final String baseUrl, @Nullable final Iterable<HttpMessageConverter<?>> converters) {
        this.baseUrl = baseUrl;
        this.converters = converters;
        this.template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
    }

    public MockRestServiceServer getServer() {
        return server;
    }

    public HttpBuilder getRestBuilder() {
        return Http.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converters(firstNonNull(converters, DEFAULT_CONVERTERS))
                .baseUrl(baseUrl);
    }
    public Http getRest() {
        return getRestBuilder().build();
    }

}
