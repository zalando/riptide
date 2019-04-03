package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.conn.HttpClientConnectionManager;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.auth.AuthorizationPlugin;
import org.zalando.riptide.auth.AuthorizationProvider;
import org.zalando.riptide.auth.PlatformCredentialsAuthorizationProvider;
import org.zalando.riptide.autoconfigure.RiptideProperties.Chaos;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;
import org.zalando.riptide.autoconfigure.RiptideProperties.OAuth;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.chaos.ChaosPlugin;
import org.zalando.riptide.chaos.ErrorResponseInjection;
import org.zalando.riptide.chaos.ExceptionInjection;
import org.zalando.riptide.chaos.LatencyInjection;
import org.zalando.riptide.chaos.Probability;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.faults.DefaultFaultClassifier;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.GzipHttpRequestInterceptor;
import org.zalando.riptide.httpclient.metrics.HttpConnectionPoolMetrics;
import org.zalando.riptide.idempotency.IdempotencyPredicate;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.zalando.tracer.concurrent.TracingExecutors;

import javax.xml.soap.SOAPConstants;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.autoconfigure.Dependencies.ifPresent;
import static org.zalando.riptide.autoconfigure.Registry.generateBeanName;
import static org.zalando.riptide.autoconfigure.Registry.list;
import static org.zalando.riptide.autoconfigure.Registry.ref;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.ErrorResponses;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.Exceptions;
import static org.zalando.riptide.autoconfigure.RiptideProperties.Chaos.Latency;

@Slf4j
@AllArgsConstructor
final class DefaultRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach(this::registerHttp);
    }

    private void registerHttp(final String id, final Client client) {
        registry.registerIfAbsent(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            return genericBeanDefinition(HttpFactory.class)
                    .setFactoryMethod("create")
                    .addConstructorArgValue(registerExecutor(id, client))
                    .addConstructorArgReference(registerClientHttpRequestFactory(id, client))
                    .addConstructorArgValue(client.getBaseUrl())
                    .addConstructorArgValue(client.getUrlResolution())
                    .addConstructorArgValue(registerHttpMessageConverters(id, client))
                    .addConstructorArgValue(registerPlugins(id, client));
        });
    }

    private String registerClientHttpRequestFactory(final String id, final Client client) {
        return registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);
            return genericBeanDefinition(ApacheClientHttpRequestFactory.class)
                    .addConstructorArgReference(registerHttpClient(id, client))
                    .addConstructorArgValue(client.getConnections().getMode());
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
                    // TODO UncaughtExceptionHandler?
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

    private BeanDefinition registerHttpMessageConverters(final String id, final Client client) {
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

            if (client.getSoap().getEnabled()) {
                final Map<String, String> protocols = ImmutableMap.of(
                        "1.1", SOAPConstants.SOAP_1_1_PROTOCOL,
                        "1.2", SOAPConstants.SOAP_1_2_PROTOCOL
                );

                final String protocol = requireNonNull(protocols.get(client.getSoap().getProtocol()),
                        "Unsupported protocol: " + client.getSoap().getProtocol());

                log.debug("Client [{}]: Registering SOAPHttpMessageConverter", id);
                list.add(genericBeanDefinition(SOAPHttpMessageConverter.class)
                        .addConstructorArgValue(protocol)
                        .getBeanDefinition());

                log.debug("Client [{]]: Registering SOAPFaultHttpMessageConverter", id);
                list.add(genericBeanDefinition(SOAPFaultHttpMessageConverter.class)
                        .addConstructorArgValue(protocol)
                        .getBeanDefinition());
            }

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

    private String findObjectMapper(final String id) {
        final String beanName = generateBeanName(id, ObjectMapper.class);
        return registry.isRegistered(beanName) ? beanName : "jacksonObjectMapper";
    }

    private List<BeanReference> registerPlugins(final String id, final Client client) {

        final Stream<Optional<String>> plugins = Stream.of(
                registerChaosPlugin(id, client),
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
                .map(Registry::ref)
                .collect(toCollection(Registry::list));
    }

    private Optional<String> registerChaosPlugin(final String id, final Client client) {
        final Chaos chaos = client.getChaos();
        final Latency latency = chaos.getLatency();
        final Exceptions exceptions = chaos.getExceptions();
        final ErrorResponses errorResponses = chaos.getErrorResponses();

        final List<String> injections = new ArrayList<>();

        if (latency.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, LatencyInjection.class, () ->
                    genericBeanDefinition(LatencyInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(id, "Latency", Probability.class, () ->
                                            genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(latency.getProbability())))
                            .addConstructorArgValue(Clock.systemUTC())
                            .addConstructorArgValue(latency.getDelay().toDuration())));
        }

        if (exceptions.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ExceptionInjection.class, () -> {
                final List<Supplier<Exception>> singleton = singletonList(SocketTimeoutException::new);
                return genericBeanDefinition(ExceptionInjection.class)
                        .addConstructorArgReference(
                                registry.registerIfAbsent(id, "Exception", Probability.class, () ->
                                        genericBeanDefinition(Probability.class)
                                                .setFactoryMethod("fixed")
                                                .addConstructorArgValue(exceptions.getProbability())))
                        .addConstructorArgValue(singleton);
            }));
        }

        if (errorResponses.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ErrorResponseInjection.class, () -> {
                return genericBeanDefinition(ErrorResponseInjection.class)
                        .addConstructorArgReference(
                                registry.registerIfAbsent(id, "ErrorResponse", Probability.class, () ->
                                        genericBeanDefinition(Probability.class)
                                                .setFactoryMethod("fixed")
                                                .addConstructorArgValue(errorResponses.getProbability())))
                        .addConstructorArgValue(errorResponses.getStatusCodes().stream()
                                .map(HttpStatus::valueOf)
                                .collect(toList()));
            }));
        }

        if (injections.isEmpty()) {
            return Optional.empty();
        }

        final String pluginId = registry.registerIfAbsent(id, ChaosPlugin.class, () ->
                genericBeanDefinition(ChaosPlugin.class)
                        .addConstructorArgValue(injections.stream()
                                .map(Registry::ref)
                                .collect(toCollection(Registry::list))));

        return Optional.of(pluginId);
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
                            .addConstructorArgValue(new IdempotencyPredicate())
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
        if (client.getTimeouts().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, TimeoutPlugin.class.getSimpleName());
            final TimeSpan timeout = client.getTimeouts().getGlobal();
            final String pluginId = registry.registerIfAbsent(id, TimeoutPlugin.class, () ->
                    genericBeanDefinition(TimeoutPlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(timeout.getAmount())
                            .addConstructorArgValue(timeout.getUnit())
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
            return registry.registerIfAbsent(FaultClassifier.class, () ->
                    genericBeanDefinition(DefaultFaultClassifier.class));
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
