package org.zalando.riptide.autoconfigure;

import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.zalando.riptide.*;

import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("unused")
final class HttpFactory {

    private HttpFactory() {

    }

    public static Http create(
            final Executor executor,
            final ClientHttpRequestFactory requestFactory,
            final String baseUrl,
            final UrlResolution urlResolution,
            final List<HttpMessageConverter<?>> converters,
            final List<Plugin> plugins) {

        return Http.builder()
                .executor(executor)
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .urlResolution(urlResolution)
                .converters(converters)
                .plugins(plugins)
                .build();
    }

}
