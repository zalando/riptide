package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.With;
import org.organicdesign.fp.collections.ImList;
import org.organicdesign.fp.collections.PersistentVector;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.zalando.riptide.Http.ConfigurationStage;
import org.zalando.riptide.Http.ExecutorStage;
import org.zalando.riptide.Http.FinalStage;
import org.zalando.riptide.Http.RequestFactoryStage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;
import static org.zalando.riptide.Plugin.composite;

@With(PRIVATE)
@AllArgsConstructor(access = PRIVATE)
final class DefaultHttpBuilder implements ExecutorStage, RequestFactoryStage, ConfigurationStage, FinalStage {

    private static class Converters {
        private static final ImmutableList<HttpMessageConverter<?>> DEFAULT =
                ImmutableList.copyOf(new RestTemplate().getMessageConverters());

        private Converters() {

        }
    }

    private static class Plugins {
        private static final ImmutableList<Plugin> DEFAULT =
                ImmutableList.copyOf(ServiceLoader.load(Plugin.class));

        private Plugins() {

        }
    }

    private static final UrlResolution DEFAULT_RESOLUTION = UrlResolution.RFC;

    private final Executor executor;
    private final IO io;
    private final ImList<HttpMessageConverter<?>> converters;
    private final Supplier<URI> baseUrl;
    private final UrlResolution resolution;
    private final ImList<Plugin> plugins;

    DefaultHttpBuilder() {
        this(null, null, PersistentVector.empty(), () -> null, DEFAULT_RESOLUTION, PersistentVector.empty());
    }

    @Override
    public RequestFactoryStage executor(final Executor executor) {
        return new DefaultHttpBuilder(executor, io, converters, baseUrl, resolution, plugins);
    }

    @Override
    public ConfigurationStage requestFactory(final ClientHttpRequestFactory factory) {
        return withIo(new BlockingIO(factory));
    }

    @Override
    public ConfigurationStage defaultConverters() {
        return converters(Converters.DEFAULT);
    }

    @Override
    public ConfigurationStage converters(@Nonnull final Iterable<HttpMessageConverter<?>> converters) {
        return withConverters(this.converters.concat(converters));
    }

    @Override
    public ConfigurationStage converter(final HttpMessageConverter<?> converter) {
        return withConverters(converters.append(converter));
    }

    @Override
    public ConfigurationStage baseUrl(@Nullable final String baseUrl) {
        return baseUrl(baseUrl == null ? null : URI.create(baseUrl));
    }

    @Override
    public ConfigurationStage baseUrl(@Nullable final URI baseUrl) {
        checkAbsoluteBaseUrl(baseUrl);
        return withBaseUrl(() -> baseUrl);
    }

    @Override
    public ConfigurationStage baseUrl(final Supplier<URI> baseUrl) {
        return withBaseUrl(() -> checkAbsoluteBaseUrl(baseUrl.get()));
    }

    private URI checkAbsoluteBaseUrl(@Nullable final URI baseUrl) {
        checkArgument(baseUrl == null || baseUrl.isAbsolute(), "Base URL is not absolute");
        return baseUrl;
    }

    @Override
    public ConfigurationStage urlResolution(@Nullable final UrlResolution resolution) {
        return withResolution(firstNonNull(resolution, DEFAULT_RESOLUTION));
    }

    @Override
    public ConfigurationStage defaultPlugins() {
        return plugins(Plugins.DEFAULT);
    }

    @Override
    public ConfigurationStage plugins(final Iterable<Plugin> plugins) {
        return withPlugins(this.plugins.concat(plugins));
    }

    @Override
    public ConfigurationStage plugin(final Plugin plugin) {
        return withPlugins(plugins.append(plugin));
    }

    @Override
    public Http build() {
        final List<HttpMessageConverter<?>> converters = converters();
        final List<Plugin> plugins = new ArrayList<>();

        if (executor != null) {
            plugins.add(new AsyncPlugin(executor));
        }

        plugins.add(new DispatchPlugin(new DefaultMessageReader(converters)));
        plugins.add(new SerializationPlugin(new DefaultMessageWriter(converters)));
        plugins.addAll(plugins());

        return new DefaultHttp(io, baseUrl, resolution, composite(plugins));
    }

    private List<HttpMessageConverter<?>> converters() {
        return converters.isEmpty() ? Converters.DEFAULT : converters;
    }

    private List<Plugin> plugins() {
        return plugins.isEmpty() ? Plugins.DEFAULT : plugins;
    }

}
