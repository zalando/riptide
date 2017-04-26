package org.zalando.riptide;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;

public final class RestBuilder {

    // package private so we can trick code coverage
    static class Converters {
        private static final ImmutableList<HttpMessageConverter<?>> DEFAULT =
                ImmutableList.copyOf(new RestTemplate().getMessageConverters());
    }

    static class Plugins {
        private static final ImmutableList<Plugin> DEFAULT =
                ImmutableList.of(new OriginalStackTracePlugin());
    }

    private AsyncClientHttpRequestFactory requestFactory;
    private final List<HttpMessageConverter<?>> converters = new ArrayList<>();
    private Supplier<URI> baseUrlProvider = () -> null;
    private final List<Plugin> plugins = new ArrayList<>();

    RestBuilder() {

    }

    public RestBuilder requestFactory(final AsyncClientHttpRequestFactory requestFactory) {
        this.requestFactory = requestFactory;
        return this;
    }

    public RestBuilder defaultConverters() {
        return converters(Converters.DEFAULT);
    }

    public RestBuilder converters(final Iterable<HttpMessageConverter<?>> converters) {
        converters.forEach(this::converter);
        return this;
    }

    public RestBuilder converter(final HttpMessageConverter<?> converter) {
        this.converters.add(converter);
        return this;
    }

    public RestBuilder baseUrl(@Nullable final String baseUrl) {
        return baseUrl(baseUrl == null ? null : URI.create(baseUrl));
    }

    public RestBuilder baseUrl(@Nullable final URI baseUrl) {
        checkAbsoluteBaseUrl(baseUrl);
        return baseUrl(() -> baseUrl);
    }

    public RestBuilder baseUrl(final Supplier<URI> baseUrlProvider) {
        this.baseUrlProvider = () -> checkAbsoluteBaseUrl(baseUrlProvider.get());
        return this;
    }

    private URI checkAbsoluteBaseUrl(@Nullable final URI baseUrl) {
        checkArgument(baseUrl == null || baseUrl.isAbsolute(), "Base URL is not absolute");
        return baseUrl;
    }

    public RestBuilder defaultPlugins() {
        return plugins(Plugins.DEFAULT);
    }

    public RestBuilder plugins(final Iterable<Plugin> plugins) {
        plugins.forEach(this::plugin);
        return this;
    }

    public RestBuilder plugin(final Plugin plugin) {
        this.plugins.add(plugin);
        return this;
    }

    public RestBuilder configure(final RestConfigurer configurer) {
        configurer.configure(this);
        return this;
    }

    public static RestConfigurer simpleRequestFactory(final ExecutorService executor) {
        return builder -> {
            final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setTaskExecutor(new ConcurrentTaskExecutor(executor));
            builder.requestFactory(factory);
        };
    }

    public Rest build() {
        return new Rest(requestFactory, converters(), baseUrlProvider, plugin());
    }

    private List<HttpMessageConverter<?>> converters() {
        return converters.isEmpty() ? Converters.DEFAULT : converters;
    }

    private Plugin plugin() {
        return plugins().stream().reduce(Plugin::merge).orElse(NoopPlugin.INSTANCE);
    }

    private List<Plugin> plugins() {
        return plugins.isEmpty() ? Plugins.DEFAULT : plugins;
    }

}
