package org.zalando.riptide.problem;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.Http;
import tools.jackson.databind.json.JsonMapper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import static com.google.common.base.MoreObjects.firstNonNull;

final class MockSetup {

    private static final List<HttpMessageConverter<?>> DEFAULT_CONVERTERS =
            Arrays.asList(new StringHttpMessageConverter(),
                    createJsonConverter());

    private static JacksonJsonHttpMessageConverter createJsonConverter() {
        var mapper = JsonMapper.builder().build();
        return new JacksonJsonHttpMessageConverter(mapper);
    }


    private final String baseUrl;
    private final Iterable<HttpMessageConverter<?>> converters;
    private final RestTemplate template;
    private final MockRestServiceServer server;

    public MockSetup() {
        this("https://api.example.com", null);
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
