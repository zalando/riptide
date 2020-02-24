package org.zalando.riptide.autoconfigure;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.zalando.riptide.Http;
import org.zalando.riptide.Http.RequestFactoryStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.UrlResolution;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executor;

@SuppressWarnings("unused")
final class HttpFactory {

    private HttpFactory() {

    }

    public static Http create(
            @Nullable final Executor executor,
            final ClientHttpRequestFactory requestFactory,
            final BaseURL baseUrl,
            final UrlResolution urlResolution,
            final List<HttpMessageConverter<?>> converters,
            final List<Plugin> plugins) {


        return configure(executor)
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .urlResolution(urlResolution)
                .converters(converters)
                .plugins(plugins)
                .build();
    }

    private static RequestFactoryStage configure(
            @Nullable final Executor executor) {

        if (executor == null) {
            return Http.builder();
        }

        return Http.builder()
                .executor(executor);
    }

}
