package org.zalando.riptide.spring;

import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;

import java.util.List;

final class HttpFactory {

    @SuppressWarnings("unused")
    public static Http create(final AsyncClientHttpRequestFactory requestFactory,
            final List<HttpMessageConverter<?>> converters, final String baseUrl, final List<Plugin> plugins) {
        return Http.builder()
                .requestFactory(requestFactory)
                .converters(converters)
                .baseUrl(baseUrl)
                .plugins(plugins)
                .build();
    }

}
