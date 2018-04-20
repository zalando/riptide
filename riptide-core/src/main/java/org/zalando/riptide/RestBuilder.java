package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.apiguardian.api.API.Status.DEPRECATED;

/**
 * @see Http#builder()
 * @see DefaultHttpBuilder
 */
@API(status = DEPRECATED, since = "2.5.0")
@Deprecated//(since = "2.5.0", forRemoval = true)
public final class RestBuilder implements HttpBuilder {

    private final HttpBuilder builder;

    RestBuilder(final HttpBuilder builder) {
        this.builder = builder;
    }

    public static RestConfigurer simpleRequestFactory(final ExecutorService executor) {
        return HttpBuilder.simpleRequestFactory(executor)::configure;
    }

    @Override
    public RestBuilder requestFactory(final AsyncClientHttpRequestFactory requestFactory) {
        return new RestBuilder(builder.requestFactory(requestFactory));
    }

    @Override
    public RestBuilder defaultConverters() {
        return new RestBuilder(builder.defaultConverters());
    }

    @Override
    public RestBuilder converters(final Iterable<HttpMessageConverter<?>> converters) {
        return new RestBuilder(builder.converters(converters));
    }

    @Override
    public RestBuilder converter(final HttpMessageConverter<?> converter) {
        return new RestBuilder(builder.converter(converter));
    }

    @Override
    public RestBuilder baseUrl(@Nullable final String baseUrl) {
        return new RestBuilder(builder.baseUrl(baseUrl));
    }

    @Override
    public RestBuilder baseUrl(@Nullable final URI baseUrl) {
        return new RestBuilder(builder.baseUrl(baseUrl));
    }

    @Override
    public RestBuilder baseUrl(final Supplier<URI> baseUrlProvider) {
        return new RestBuilder(builder.baseUrl(baseUrlProvider));
    }

    @Override
    public RestBuilder urlResolution(@Nullable final UrlResolution resolution) {
        return new RestBuilder(builder.urlResolution(resolution));
    }

    @Override
    public RestBuilder defaultPlugins() {
        return new RestBuilder(builder.defaultPlugins());
    }

    @Override
    public RestBuilder plugins(final Iterable<Plugin> plugins) {
        return new RestBuilder(builder.plugins(plugins));
    }

    @Override
    public RestBuilder plugin(final Plugin plugin) {
        return new RestBuilder(builder.plugin(plugin));
    }

    @Override
    public RestBuilder configure(final HttpConfigurer configurer) {
        return new RestBuilder(builder.configure(configurer));
    }

    @Override
    public Rest build() {
        return new Rest(builder.build());
    }

}
