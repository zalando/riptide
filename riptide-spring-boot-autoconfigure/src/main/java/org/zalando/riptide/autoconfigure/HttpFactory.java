package org.zalando.riptide.autoconfigure;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.UrlResolution;

import java.util.List;
import java.util.concurrent.Executor;

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
