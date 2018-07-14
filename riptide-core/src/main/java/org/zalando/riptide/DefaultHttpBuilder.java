package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.zalando.riptide.Plugin.compound;

final class DefaultHttpBuilder implements HttpBuilder {

    // package private so we can trick code coverage
    static class Converters {
        private static final ImmutableList<HttpMessageConverter<?>> DEFAULT =
                ImmutableList.copyOf(new RestTemplate().getMessageConverters());

        private Converters() {

        }
    }

    static class Plugins {
        private static final ImmutableList<Plugin> DEFAULT =
                ImmutableList.of(new OriginalStackTracePlugin());

        private Plugins() {

        }
    }

    private static final UrlResolution DEFAULT_RESOLUTION = UrlResolution.RFC;

    private AsyncClientHttpRequestFactory requestFactory;
    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();
    private Supplier<URI> baseUrlProvider = () -> null;
    private UrlResolution resolution = DEFAULT_RESOLUTION;
    private final List<Plugin> plugins = new ArrayList<>();

    DefaultHttpBuilder() {

    }

    @Override
    public HttpBuilder requestFactory(final AsyncClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
        return this;
    }

    @Override
    public HttpBuilder defaultConverters() {
        return converters(Converters.DEFAULT);
    }

    @Override
    public HttpBuilder converters(final Iterable<HttpMessageConverter<?>> converters) {
        converters.forEach(this::converter);
        return this;
    }

    @Override
    public HttpBuilder converter(final HttpMessageConverter<?> converter) {
        this.converters.add(converter);
        return this;
    }

    @Override
    public HttpBuilder baseUrl(@Nullable final String baseUrl) {
        return baseUrl(baseUrl == null ? null : URI.create(baseUrl));
    }

    @Override
    public HttpBuilder baseUrl(@Nullable final URI baseUrl) {
        checkAbsoluteBaseUrl(baseUrl);
        return baseUrl(() -> baseUrl);
    }

    @Override
    public HttpBuilder baseUrl(final Supplier<URI> baseUrlProvider) {
        this.baseUrlProvider = () -> checkAbsoluteBaseUrl(baseUrlProvider.get());
        return this;
    }

    private URI checkAbsoluteBaseUrl(@Nullable final URI baseUrl) {
        checkArgument(baseUrl == null || baseUrl.isAbsolute(), "Base URL is not absolute");
        return baseUrl;
    }

    @Override
    public HttpBuilder urlResolution(@Nullable final UrlResolution resolution) {
        this.resolution = firstNonNull(resolution, DEFAULT_RESOLUTION);
        return this;
    }

    @Override
    public HttpBuilder defaultPlugins() {
        return plugins(Plugins.DEFAULT);
    }

    @Override
    public HttpBuilder plugins(final Iterable<Plugin> plugins) {
        plugins.forEach(this::plugin);
        return this;
    }

    @Override
    public HttpBuilder plugin(final Plugin plugin) {
        this.plugins.add(plugin);
        return this;
    }

    @Override
    public HttpBuilder configure(final HttpConfigurer configurer) {
        configurer.configure(this);
        return this;
    }

    @Override
    public Http build() {
        return new DefaultHttp(requestFactory, converters(), baseUrlProvider, resolution, compound(plugins()));
    }

    private List<HttpMessageConverter<?>> converters() {
        return converters.isEmpty() ? Converters.DEFAULT : converters;
    }

    private List<Plugin> plugins() {
        return plugins.isEmpty() ? Plugins.DEFAULT : plugins;
    }

}
