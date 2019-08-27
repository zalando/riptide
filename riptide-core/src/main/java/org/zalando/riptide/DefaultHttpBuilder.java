package org.zalando.riptide;

import com.google.common.collect.*;
import lombok.*;
import org.organicdesign.fp.collections.*;
import org.springframework.http.client.*;
import org.springframework.http.converter.*;
import org.springframework.web.client.*;
import org.zalando.riptide.Http.*;

import javax.annotation.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static org.zalando.riptide.Plugin.*;

@AllArgsConstructor
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
        final IO io = new BlockingIO(factory);
        return new DefaultHttpBuilder(executor, io, converters, baseUrl, resolution, plugins);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ConfigurationStage asyncRequestFactory(final AsyncClientHttpRequestFactory factory) {
        final Executor executor = Runnable::run;
        final IO io = new NonBlockingIO(factory);
        return new DefaultHttpBuilder(executor, io, converters, baseUrl, resolution, plugins);
    }

    @Override
    public ConfigurationStage defaultConverters() {
        return converters(Converters.DEFAULT);
    }

    @Override
    public ConfigurationStage converters(@Nonnull final Iterable<HttpMessageConverter<?>> converters) {
        return new DefaultHttpBuilder(executor, io, this.converters.concat(converters), baseUrl, resolution, plugins);
    }

    @Override
    public ConfigurationStage converter(final HttpMessageConverter<?> converter) {
        return new DefaultHttpBuilder(executor, io, converters.append(converter), baseUrl, resolution, plugins);
    }

    @Override
    public ConfigurationStage baseUrl(@Nullable final String baseUrl) {
        return baseUrl(baseUrl == null ? null : URI.create(baseUrl));
    }

    @Override
    public ConfigurationStage baseUrl(@Nullable final URI baseUrl) {
        checkAbsoluteBaseUrl(baseUrl);
        return baseUrl(() -> baseUrl);
    }

    @Override
    public ConfigurationStage baseUrl(final Supplier<URI> baseUrl) {
        return new DefaultHttpBuilder(executor, io, converters,
                () -> checkAbsoluteBaseUrl(baseUrl.get()), resolution, plugins);
    }

    private URI checkAbsoluteBaseUrl(@Nullable final URI baseUrl) {
        checkArgument(baseUrl == null || baseUrl.isAbsolute(), "Base URL is not absolute");
        return baseUrl;
    }

    @Override
    public ConfigurationStage urlResolution(@Nullable final UrlResolution resolution) {
        return new DefaultHttpBuilder(executor, io, converters, baseUrl,
                firstNonNull(resolution, DEFAULT_RESOLUTION), plugins);
    }

    @Override
    public ConfigurationStage defaultPlugins() {
        return plugins(Plugins.DEFAULT);
    }

    @Override
    public ConfigurationStage plugins(final Iterable<Plugin> plugins) {
        return new DefaultHttpBuilder(executor, io, converters, baseUrl, resolution,
                this.plugins.concat(plugins));
    }

    @Override
    public ConfigurationStage plugin(final Plugin plugin) {
        return new DefaultHttpBuilder(executor, io, converters, baseUrl, resolution,
                plugins.append(plugin));
    }

    @Override
    public Http build() {
        final List<HttpMessageConverter<?>> converters = converters();

        final List<Plugin> plugins = new ArrayList<>();
        plugins.add(new AsyncPlugin(executor));
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
