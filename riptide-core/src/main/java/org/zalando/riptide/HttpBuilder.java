package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface HttpBuilder {

    static HttpConfigurer simpleRequestFactory(final ExecutorService executor) {
        return builder -> {
            final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setTaskExecutor(new ConcurrentTaskExecutor(executor));
            builder.requestFactory(factory);
        };
    }

    HttpBuilder requestFactory(AsyncClientHttpRequestFactory requestFactory);

    HttpBuilder defaultConverters();

    HttpBuilder converters(Iterable<HttpMessageConverter<?>> converters);

    HttpBuilder converter(HttpMessageConverter<?> converter);

    HttpBuilder baseUrl(@Nullable String baseUrl);

    HttpBuilder baseUrl(@Nullable URI baseUrl);

    HttpBuilder baseUrl(Supplier<URI> baseUrlProvider);

    HttpBuilder urlResolution(@Nullable UrlResolution resolution);

    HttpBuilder defaultPlugins();

    HttpBuilder plugins(Iterable<Plugin> plugins);

    HttpBuilder plugin(Plugin plugin);

    HttpBuilder configure(HttpConfigurer configurer);

    Http build();
}
