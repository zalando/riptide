package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.ConnectionClosedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.conn.HttpClientConnectionManager;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.PluginInterceptor;
import org.zalando.riptide.auth.AuthorizationPlugin;
import org.zalando.riptide.auth.AuthorizationProvider;
import org.zalando.riptide.auth.PlatformCredentialsAuthorizationProvider;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.OAuth;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.GzipHttpRequestInterceptor;
import org.zalando.riptide.httpclient.metrics.HttpConnectionPoolMetrics;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.zalando.tracer.concurrent.TracingExecutors;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.autoconfigure.Dependencies.ifPresent;
import static org.zalando.riptide.autoconfigure.Registry.generateBeanName;
import static org.zalando.riptide.autoconfigure.Registry.list;
import static org.zalando.riptide.autoconfigure.Registry.ref;

@Slf4j
@AllArgsConstructor
final class DefaultRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach((id, client) -> {
            final String factoryId = registerAsyncClientHttpRequestFactory(id, client);
            final BeanDefinition converters = registerHttpMessageConverters(id);
            final List<String> plugins = registerPlugins(id, client);

            registerHttp(id, client, factoryId, converters, plugins);
            registerRestTemplate(id, factoryId, client, converters, plugins);
            registerAsyncRestTemplate(id, factoryId, client, converters, plugins);
        });
    }

    private String registerAsyncClientHttpRequestFactory(final String id, final Client client) {
        return registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);
            return genericBeanDefinition(ApacheClientHttpRequestFactory.class)
                    .addConstructorArgReference(registerHttpClient(id, client));
        });
    }

    private BeanMetadataElement registerExecutor(final String id, final Client client) {
        final String name = "http-" + id;

        final String executorId = registry.registerIfAbsent(id, ExecutorService.class, () -> {
            final RiptideProperties.Threads threads = client.getThreads();
            return genericBeanDefinition(ThreadPoolExecutor.class)
                    .addConstructorArgValue(threads.getMinSize())
                    .addConstructorArgValue(threads.getMaxSize())
                    .addConstructorArgValue(threads.getKeepAlive().getAmount())
                    .addConstructorArgValue(threads.getKeepAlive().getUnit())
                    .addConstructorArgValue(threads.getQueueSize() == 0 ?
                            new SynchronousQueue<>() :
                            new ArrayBlockingQueue<>(threads.getQueueSize()))
                    .addConstructorArgValue(new CustomizableThreadFactory(name + "-"))
                    .setDestroyMethodName("shutdown");
        });

        if (client.getMetrics().getEnabled()) {
            registry.registerIfAbsent(id, ExecutorServiceMetrics.class, () ->
                    genericBeanDefinition(ExecutorServiceMetrics.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgValue(name)
                            .addConstructorArgValue(ImmutableList.of(clientId(id))));
        }

        return trace(executorId);
    }

    private static final class HttpMessageConverters {

    }

    private BeanDefinition registerHttpMessageConverters(final String id) {
        // we use the wrong type here since that's the easiest way to influence the name
        // we want exampleHttpMessageConverters, rather than exampleClientHttpMessageConverters

        final String convertersId = registry.registerIfAbsent(id, HttpMessageConverters.class, () -> {
            final List<Object> list = list();

            final String objectMapperId = findObjectMapper(id);

            log.debug("Client [{}]: Registering MappingJackson2HttpMessageConverter referencing [{}]", id,
                    objectMapperId);
            list.add(genericBeanDefinition(MappingJackson2HttpMessageConverter.class)
                    .addConstructorArgReference(objectMapperId)
                    .getBeanDefinition());

            ifPresent("org.zalando.riptide.stream.Streams", () -> {
                log.debug("Client [{}]: Registering StreamConverter referencing [{}]", id, objectMapperId);
                list.add(genericBeanDefinition(Streams.class)
                        .setFactoryMethod("streamConverter")
                        .addConstructorArgReference(objectMapperId)
                        .getBeanDefinition());
            });

            log.debug("Client [{}]: Registering StringHttpMessageConverter", id);
            list.add(genericBeanDefinition(StringHttpMessageConverter.class)
                    .addPropertyValue("writeAcceptCharset", false)
                    .getBeanDefinition());

            return genericBeanDefinition(ClientHttpMessageConverters.class)
                    .addConstructorArgValue(list);
        });

        return genericBeanDefinition()
                .setFactoryMethodOnBean("getConverters", convertersId)
                .getBeanDefinition();
    }

    private void registerHttp(final String id, final Client client, final String factoryId,
            final BeanDefinition converters, final List<String> plugins) {
        registry.registerIfAbsent(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            return genericBeanDefinition(HttpFactory.class)
                    .setFactoryMethod("create")
                    .addConstructorArgValue(registerExecutor(id, client))
                    .addConstructorArgReference(factoryId)
                    .addConstructorArgValue(client.getBaseUrl())
                    .addConstructorArgValue(client.getUrlResolution())
                    .addConstructorArgValue(converters)
                    .addConstructorArgValue(plugins.stream()
                            .map(Registry::ref)
                            .collect(toCollection(Registry::list)));
        });
    }

    private void registerRestTemplate(final String id, final String factoryId, final Client client,
            final BeanDefinition converters, final List<String> plugins) {
        registry.registerIfAbsent(id, RestTemplate.class, () -> {
            log.debug("Client [{}]: Registering RestTemplate", id);

            final BeanDefinitionBuilder template = genericBeanDefinition(RestTemplate.class);
            template.addConstructorArgReference(factoryId);
            configureTemplate(template, client.getBaseUrl(), converters, plugins);

            return template;
        });
    }

    private void registerAsyncRestTemplate(final String id, final String factoryId, final Client client,
            final BeanDefinition converters, final List<String> plugins) {
        registry.registerIfAbsent(id, AsyncRestTemplate.class, () -> {
            log.debug("Client [{}]: Registering AsyncRestTemplate", id);

            final BeanDefinitionBuilder template = genericBeanDefinition(AsyncRestTemplate.class);
            template.addConstructorArgReference(registry.registerIfAbsent(id, AsyncClientHttpRequestFactory.class, () ->
                    genericBeanDefinition(ConcurrentClientHttpRequestFactory.class)
                            .addConstructorArgReference(factoryId)
                            .addConstructorArgValue(genericBeanDefinition(ConcurrentTaskExecutor.class)
                                    .addConstructorArgValue(registerExecutor(id, client))
                                    .getBeanDefinition())));
            template.addConstructorArgReference(factoryId);
            configureTemplate(template, client.getBaseUrl(), converters, plugins);

            return template;
        });
    }

    private void configureTemplate(final BeanDefinitionBuilder template, @Nullable final String baseUrl,
            final BeanDefinition converters,
            final List<String> plugins) {
        final DefaultUriBuilderFactory factory = baseUrl == null ?
                new DefaultUriBuilderFactory() :
                new DefaultUriBuilderFactory(baseUrl);
        template.addPropertyValue("uriTemplateHandler", factory);
        template.addPropertyValue("messageConverters", converters);
        template.addPropertyValue("interceptors", plugins.stream()
                .map(plugin -> genericBeanDefinition(PluginInterceptor.class)
                        .addConstructorArgReference(plugin)
                        .getBeanDefinition())
                .collect(toCollection(Registry::list)));
    }

    private String findObjectMapper(final String id) {
        final String beanName = generateBeanName(id, ObjectMapper.class);
        return registry.isRegistered(beanName) ? beanName : "jacksonObjectMapper";
    }

    private List<String> registerPlugins(final String id, final Client client) {

        final Stream<Optional<String>> plugins = Stream.of(
                registerMetricsPlugin(id, client),
                registerTransientFaultPlugin(id, client),
                registerFailsafePlugin(id, client),
                registerBackupPlugin(id, client),
                registerAuthorizationPlugin(id, client),
                registerTimeoutPlugin(id, client),
                registerOriginalStackTracePlugin(id, client),
                registerCustomPlugin(id));

        return plugins
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toCollection(Registry::list));
    }

    private Optional<String> registerMetricsPlugin(final String id, final Client client) {
        if (client.getMetrics().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, MetricsPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, MetricsPlugin.class, () ->
                    genericBeanDefinition(MetricsPluginFactory.class)
                            .setFactoryMethod("createMetricsPlugin")
                            .addConstructorArgReference("meterRegistry")
                            .addConstructorArgValue(ImmutableList.of(clientId(id))));

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerTransientFaultPlugin(final String id, final Client client) {
        if (client.getTransientFaultDetection().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, TransientFaultPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, TransientFaultPlugin.class, () ->
                    genericBeanDefinition(TransientFaultPlugin.class)
                            .addConstructorArgReference(findFaultClassifier(id)));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerFailsafePlugin(final String id, final Client client) {
        if (client.getRetry().getEnabled() || client.getCircuitBreaker().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, FailsafePlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, FailsafePlugin.class, () ->
                    genericBeanDefinition(FailsafePluginFactory.class)
                            .setFactoryMethod("createFailsafePlugin")
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(registerRetryPolicy(id, client))
                            .addConstructorArgValue(registerCircuitBreaker(id, client))
                            .addConstructorArgReference(registerRetryListener(id, client)));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerBackupPlugin(final String id, final Client client) {
        if (client.getBackupRequest().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, BackupRequestPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, BackupRequestPlugin.class, () ->
                    genericBeanDefinition(BackupRequestPlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getAmount())
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getUnit())
                            .addConstructorArgValue(registerExecutor(id, client)));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerAuthorizationPlugin(final String id, final Client client) {
        if (client.getOauth().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, AuthorizationPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, AuthorizationPlugin.class, () ->
                    genericBeanDefinition(AuthorizationPlugin.class)
                            .addConstructorArgReference(registerAuthorizationProvider(id, client.getOauth())));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerTimeoutPlugin(final String id, final Client client) {
        if (client.getTimeout() != null) {
            log.debug("Client [{}]: Registering [{}]", id, TimeoutPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, TimeoutPlugin.class, () ->
                    genericBeanDefinition(TimeoutPlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(client.getTimeout().getAmount())
                            .addConstructorArgValue(client.getTimeout().getUnit())
                            .addConstructorArgValue(registerExecutor(id, client)));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerOriginalStackTracePlugin(final String id, final Client client) {
        if (client.getStackTracePreservation().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, OriginalStackTracePlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, OriginalStackTracePlugin.class);
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerCustomPlugin(final String id) {
        if (registry.isRegistered(id, Plugin.class)) {
            final String pluginId = generateBeanName(id, Plugin.class);
            log.debug("Client [{}]: Registering [{}]", pluginId);
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private String findFaultClassifier(final String id) {
        if (registry.isRegistered(id, FaultClassifier.class)) {
            return generateBeanName(id, FaultClassifier.class);
        } else {
            return registry.registerIfAbsent(FaultClassifier.class, () -> {
                final List<Predicate<Throwable>> predicates = list();

                predicates.addAll(FaultClassifier.defaults());
                predicates.add(ConnectionClosedException.class::isInstance);
                predicates.add(NoHttpResponseException.class::isInstance);

                return genericBeanDefinition(FaultClassifier.class)
                        .setFactoryMethod("create")
                        .addConstructorArgValue(predicates);
            });
        }
    }

    private BeanMetadataElement registerScheduler(final String id, final Client client) {
        // we allow users to use their own ScheduledExecutorService, but they don't have to configure tracing
        final String name = "http-" + id + "-scheduler";

        final String executorId = registry.registerIfAbsent(id, ScheduledExecutorService.class, () -> {
            final CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(name + "-");
            threadFactory.setDaemon(true);

            return genericBeanDefinition(ScheduledThreadPoolExecutor.class)
                    .addConstructorArgValue(client.getThreads().getMaxSize())
                    .addConstructorArgValue(threadFactory)
                    .addPropertyValue("removeOnCancelPolicy", true)
                    .setDestroyMethodName("shutdown");
        });

        if (client.getMetrics().getEnabled()) {
            final String beanName = generateBeanName(id, "ScheduledExecutorServiceMetrics");

            registry.registerIfAbsent(id, beanName, () ->
                    genericBeanDefinition(ExecutorServiceMetrics.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgValue(name)
                            .addConstructorArgValue(ImmutableList.of(clientId(id))));
        }

        return trace(executorId);
    }

    private BeanMetadataElement registerRetryPolicy(final String id, final Client client) {
        if (client.getRetry().getEnabled()) {
            return ref(registry.registerIfAbsent(id, RetryPolicy.class, () ->
                    genericBeanDefinition(FailsafePluginFactory.class)
                            .setFactoryMethod("createRetryPolicy")
                            .addConstructorArgValue(client.getRetry())));
        }
        return null;

    }

    private BeanMetadataElement registerCircuitBreaker(final String id, final Client client) {
        if (client.getCircuitBreaker().getEnabled()) {
            return ref(registry.registerIfAbsent(id, CircuitBreaker.class, () ->
                    genericBeanDefinition(FailsafePluginFactory.class)
                            .setFactoryMethod("createCircuitBreaker")
                            .addConstructorArgValue(client)
                            .addConstructorArgReference(registerCircuitBreakerListener(id, client))));
        }
        return null;

    }

    private String registerRetryListener(final String id, final Client client) {
        return registry.registerIfAbsent(id, RetryListener.class, () -> {
            if (client.getMetrics().getEnabled()) {
                return genericBeanDefinition(MetricsPluginFactory.class)
                        .setFactoryMethod("createRetryListener")
                        .addConstructorArgReference("meterRegistry")
                        .addConstructorArgValue(ImmutableList.of(clientId(id)));
            } else {
                return genericBeanDefinition(MetricsPluginFactory.class)
                        .setFactoryMethod("getDefaultRetryListener");
            }
        });
    }

    private String registerCircuitBreakerListener(final String id, final Client client) {
        return registry.registerIfAbsent(id, CircuitBreakerListener.class, () -> {
            if (client.getMetrics().getEnabled()) {
                return genericBeanDefinition(MetricsPluginFactory.class)
                        .setFactoryMethod("createCircuitBreakerListener")
                        .addConstructorArgReference("meterRegistry")
                        .addConstructorArgValue(ImmutableList.of(clientId(id), clientName(id, client)));
            } else {
                return genericBeanDefinition(MetricsPluginFactory.class)
                        .setFactoryMethod("getDefaultCircuitBreakerListener");
            }
        });
    }

    private Tag clientId(final String id) {
        return Tag.of("clientId", id);
    }

    private Tag clientName(final String id, final Client client) {
        return Tag.of("clientName", getHost(client).orElse(id));
    }

    private Optional<String> getHost(final Client client) {
        return Optional.ofNullable(client.getBaseUrl())
                .map(URI::create).map(URI::getHost);
    }

    private String registerAuthorizationProvider(final String id, final OAuth oauth) {
        return registry.registerIfAbsent(id, AuthorizationProvider.class, () ->
                genericBeanDefinition(PlatformCredentialsAuthorizationProvider.class)
                        .addConstructorArgValue(oauth.getCredentialsDirectory())
                        .addConstructorArgValue(id));
    }

    private BeanMetadataElement trace(final String executor) {
        final Optional<BeanMetadataElement> result = ifPresent("org.zalando.tracer.concurrent.TracingExecutors",
                () -> {
                    if (registry.isRegistered("tracer")) {
                        return genericBeanDefinition(TracingExecutors.class)
                                .setFactoryMethod("preserve")
                                .addConstructorArgReference(executor)
                                .addConstructorArgReference("tracer")
                                .getBeanDefinition();
                    } else {
                        return null;
                    }
                });

        return result.orElseGet(() -> ref(executor));
    }

    private String registerHttpClient(final String id, final Client client) {
        return registry.registerIfAbsent(id, HttpClient.class, () -> {
            log.debug("Client [{}]: Registering HttpClient", id);

            final String connectionManager = registry.registerIfAbsent(id, HttpClientConnectionManager.class, () ->
                    genericBeanDefinition(HttpClientFactory.class)
                            .setFactoryMethod("createHttpClientConnectionManager")
                            .addConstructorArgValue(client));

            if (client.getMetrics().getEnabled()) {
                registry.registerIfAbsent(id, HttpConnectionPoolMetrics.class, () ->
                        genericBeanDefinition(HttpConnectionPoolMetrics.class)
                                .addConstructorArgReference(connectionManager)
                                .addConstructorArgValue("http.client.connections")
                                .addConstructorArgValue(ImmutableList.of(clientId(id))));
            }

            return genericBeanDefinition(HttpClientFactory.class)
                    .setFactoryMethod("createHttpClient")
                    .addConstructorArgValue(client)
                    .addConstructorArgValue(configureFirstRequestInterceptors(id, client))
                    .addConstructorArgValue(configureLastRequestInterceptors(id, client))
                    .addConstructorArgValue(configureLastResponseInterceptors(id))
                    .addConstructorArgReference(connectionManager)
                    .addConstructorArgValue(registry.isRegistered(id, HttpClientCustomizer.class) ?
                            ref(generateBeanName(id, HttpClientCustomizer.class)) : null)
                    .addConstructorArgValue(registry.isRegistered(id, HttpCacheStorage.class) ?
                            ref(generateBeanName(id, HttpCacheStorage.class)) : null)
                    .setDestroyMethodName("close");
        });
    }

    private List<BeanMetadataElement> configureFirstRequestInterceptors(final String id, final Client client) {
        final List<BeanMetadataElement> interceptors = list();

        if (registry.isRegistered("tracerHttpRequestInterceptor")) {
            log.debug("Client [{}]: Registering TracerHttpRequestInterceptor", id);
            interceptors.add(ref("tracerHttpRequestInterceptor"));
        }

        return interceptors;
    }

    private List<BeanMetadataElement> configureLastRequestInterceptors(final String id, final Client client) {
        final List<BeanMetadataElement> interceptors = list();

        if (registry.isRegistered("logbookHttpRequestInterceptor")) {
            log.debug("Client [{}]: Registering LogbookHttpRequestInterceptor", id);
            interceptors.add(ref("logbookHttpRequestInterceptor"));
        }

        if (client.getRequestCompression().getEnabled()) {
            log.debug("Client [{}]: Registering GzippingHttpRequestInterceptor", id);
            interceptors.add(genericBeanDefinition(GzipHttpRequestInterceptor.class)
                    .getBeanDefinition());
        }

        return interceptors;
    }

    private List<BeanMetadataElement> configureLastResponseInterceptors(final String id) {
        final List<BeanMetadataElement> interceptors = list();

        if (registry.isRegistered("logbookHttpResponseInterceptor")) {
            log.debug("Client [{}]: Registering LogbookHttpResponseInterceptor", id);
            interceptors.add(ref("logbookHttpResponseInterceptor"));
        }

        return interceptors;
    }

}
