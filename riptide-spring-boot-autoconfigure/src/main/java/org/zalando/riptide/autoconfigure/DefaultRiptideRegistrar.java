package org.zalando.riptide.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedScheduledExecutorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.conn.HttpClientConnectionManager;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.AsyncRestOperations;
import org.springframework.web.client.RestOperations;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestCompressionPlugin;
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
import org.zalando.riptide.compatibility.AsyncHttpOperations;
import org.zalando.riptide.compatibility.HttpOperations;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.faults.DefaultFaultClassifier;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.ApacheClientHttpRequestFactory;
import org.zalando.riptide.httpclient.metrics.HttpConnectionPoolMetrics;
import org.zalando.riptide.idempotency.IdempotencyPredicate;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.opentracing.OpenTracingPlugin;
import org.zalando.riptide.opentracing.span.SpanDecorator;
import org.zalando.riptide.soap.SOAPFaultHttpMessageConverter;
import org.zalando.riptide.soap.SOAPHttpMessageConverter;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;

import javax.xml.soap.SOAPConstants;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
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

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.autoconfigure.Dependencies.ifPresent;
import static org.zalando.riptide.autoconfigure.Name.name;
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
        properties.getClients().forEach((id, client) -> {
            registerHttp(id, client);
            registerHttpOperations(id, client);
            registerAsyncHttpOperations(id, client);
        });
    }

    private String registerHttp(final String id, final Client client) {
        return registry.registerIfAbsent(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            return genericBeanDefinition(HttpFactory.class)
                    .setFactoryMethod("create")
                    .addConstructorArgReference(registerExecutor(id, client))
                    .addConstructorArgReference(registerClientHttpRequestFactory(id, client))
                    .addConstructorArgValue(client.getBaseUrl())
                    .addConstructorArgValue(client.getUrlResolution())
                    .addConstructorArgValue(registerHttpMessageConverters(id, client))
                    .addConstructorArgValue(registerPlugins(id, client));
        });
    }

    private void registerHttpOperations(final String id, final Client client) {
        registry.registerIfAbsent(id, RestOperations.class, () ->
                genericBeanDefinition(HttpOperations.class)
                        .addConstructorArgReference(registerHttp(id, client)));
    }

    private void registerAsyncHttpOperations(final String id, final Client client) {
        registry.registerIfAbsent(id, AsyncRestOperations.class, () ->
                genericBeanDefinition(AsyncHttpOperations.class)
                        .addConstructorArgReference(registerHttp(id, client)));
    }

    private String registerClientHttpRequestFactory(final String id, final Client client) {
        return registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);
            return genericBeanDefinition(ApacheClientHttpRequestFactory.class)
                    .addConstructorArgReference(registerHttpClient(id, client))
                    .addConstructorArgValue(client.getConnections().getMode());
        });
    }

    private String registerExecutor(final String id, final Client client) {
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

        if (client.getTracing().getEnabled()) {
            final String tracerRef = registry.find(Tracer.class).orElseThrow(() -> new NoSuchBeanDefinitionException(Tracer.class));
            return registry.registerIfAbsent(id, TracedExecutorService.class, () ->
                    genericBeanDefinition(TracedExecutorService.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgReference(tracerRef));
        }

        return executorId;
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

                log.debug("Client [{}]: Registering SOAPFaultHttpMessageConverter", id);
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
        return registry.find(id, ObjectMapper.class).orElse("jacksonObjectMapper");
    }

    private List<BeanReference> registerPlugins(final String id, final Client client) {
        final Stream<Optional<String>> plugins = Stream.of(
                registerChaosPlugin(id, client),
                registerMetricsPlugin(id, client),
                registerRequestCompressionPlugin(id, client),
                registerLogbookPlugin(id, client),
                registerTransientFaultPlugin(id, client),
                registerOpenTracingPlugin(id, client),
                registerFailsafePlugin(id, client),
                registerAuthorizationPlugin(id, client),
                registerBackupPlugin(id, client),
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
                                    registry.registerIfAbsent(name(id, LatencyInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(latency.getProbability())))
                            .addConstructorArgValue(Clock.systemUTC())
                            .addConstructorArgValue(latency.getDelay().toDuration())));
        }

        if (exceptions.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ExceptionInjection.class, () ->
                    genericBeanDefinition(ExceptionInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(name(id, ExceptionInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(exceptions.getProbability())))
                            .addConstructorArgValue(
                                    Collections.<Supplier<Exception>>singletonList(SocketTimeoutException::new))));
        }

        if (errorResponses.getEnabled()) {
            injections.add(registry.registerIfAbsent(id, ErrorResponseInjection.class, () ->
                    genericBeanDefinition(ErrorResponseInjection.class)
                            .addConstructorArgReference(
                                    registry.registerIfAbsent(name(id, ErrorResponseInjection.class, Probability.class),
                                            () -> genericBeanDefinition(Probability.class)
                                                    .setFactoryMethod("fixed")
                                                    .addConstructorArgValue(errorResponses.getProbability())))
                            .addConstructorArgValue(errorResponses.getStatusCodes().stream()
                                    .map(HttpStatus::valueOf)
                                    .collect(toList()))));
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
            final String pluginId = registry.registerIfAbsent(id, MetricsPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, MetricsPlugin.class.getSimpleName());
                return genericBeanDefinition(MetricsPluginFactory.class)
                        .setFactoryMethod("createMetricsPlugin")
                        .addConstructorArgReference("meterRegistry")
                        .addConstructorArgValue(ImmutableList.of(clientId(id)));
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerLogbookPlugin(final String id, final Client client) {
        if (client.getLogging().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, LogbookPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, LogbookPlugin.class.getSimpleName());
                return genericBeanDefinition(LogbookPlugin.class)
                        .addConstructorArgReference("logbook");
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerRequestCompressionPlugin(final String id, final Client client) {
        if (client.getRequestCompression().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, RequestCompressionPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, RequestCompressionPlugin.class);
                return genericBeanDefinition(RequestCompressionPlugin.class);
            });

            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerTransientFaultPlugin(final String id, final Client client) {
        if (client.getTransientFaultDetection().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, TransientFaultPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, TransientFaultPlugin.class.getSimpleName());
                return genericBeanDefinition(TransientFaultPlugin.class)
                        .addConstructorArgReference(findFaultClassifier(id));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerOpenTracingPlugin(final String id, final Client client) {
        if (client.getTracing().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, OpenTracingPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, OpenTracingPlugin.class.getSimpleName());
                final String tracerRef = registry.find(Tracer.class).orElseThrow(() -> new NoSuchBeanDefinitionException(Tracer.class));
                return genericBeanDefinition(OpenTracingPluginFactory.class)
                        .setFactoryMethod("create")
                        .addConstructorArgReference(tracerRef)
                        .addConstructorArgValue(client)
                        .addConstructorArgValue(registry.findRef(id, SpanDecorator.class).orElse(null));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerFailsafePlugin(final String id, final Client client) {
        if (client.getRetry().getEnabled() || client.getCircuitBreaker().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, FailsafePlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, FailsafePlugin.class.getSimpleName());
                return genericBeanDefinition(FailsafePluginFactory.class)
                        .setFactoryMethod("create")
                        .addConstructorArgReference(registerScheduler(id, client))
                        .addConstructorArgValue(registerRetryPolicy(id, client))
                        .addConstructorArgValue(registerCircuitBreaker(id, client))
                        .addConstructorArgReference(registerRetryListener(id, client));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerAuthorizationPlugin(final String id, final Client client) {
        if (client.getOauth().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, AuthorizationPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, AuthorizationPlugin.class.getSimpleName());
                return genericBeanDefinition(AuthorizationPlugin.class)
                        .addConstructorArgReference(registerAuthorizationProvider(id, client.getOauth()));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerBackupPlugin(final String id, final Client client) {
        if (client.getBackupRequest().getEnabled()) {
            log.debug("Client [{}]: Registering [{}]", id, BackupRequestPlugin.class.getSimpleName());
            final String pluginId = registry.registerIfAbsent(id, BackupRequestPlugin.class, () ->
                    genericBeanDefinition(BackupRequestPlugin.class)
                            .addConstructorArgReference(registerScheduler(id, client))
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getAmount())
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getUnit())
                            .addConstructorArgValue(new IdempotencyPredicate())
                            .addConstructorArgReference(registerExecutor(id, client)));
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerTimeoutPlugin(final String id, final Client client) {
        if (client.getTimeouts().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, TimeoutPlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, TimeoutPlugin.class.getSimpleName());
                final TimeSpan timeout = client.getTimeouts().getGlobal();
                return genericBeanDefinition(TimeoutPlugin.class)
                        .addConstructorArgReference(registerScheduler(id, client))
                        .addConstructorArgValue(timeout.getAmount())
                        .addConstructorArgValue(timeout.getUnit())
                        .addConstructorArgReference(registerExecutor(id, client));
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerOriginalStackTracePlugin(final String id, final Client client) {
        if (client.getStackTracePreservation().getEnabled()) {
            final String pluginId = registry.registerIfAbsent(id, OriginalStackTracePlugin.class, () -> {
                log.debug("Client [{}]: Registering [{}]", id, OriginalStackTracePlugin.class.getSimpleName());
                return genericBeanDefinition(OriginalStackTracePlugin.class);
            });
            return Optional.of(pluginId);
        }
        return Optional.empty();
    }

    private Optional<String> registerCustomPlugin(final String id) {
        return registry.find(id, Plugin.class);
    }

    private String findFaultClassifier(final String id) {
        return registry.find(id, FaultClassifier.class).orElseGet(() ->
                registry.registerIfAbsent(name(FaultClassifier.class), () ->
                        genericBeanDefinition(DefaultFaultClassifier.class)));
    }

    private String registerScheduler(final String id, final Client client) {
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
            registry.registerIfAbsent(name(id, "Scheduled", ExecutorServiceMetrics.class), () ->
                    genericBeanDefinition(ExecutorServiceMetrics.class)
                            .addConstructorArgReference(executorId)
                            .addConstructorArgValue(name)
                            .addConstructorArgValue(ImmutableList.of(clientId(id))));
        }

        if (client.getTracing().getEnabled()) {
            return registry.registerIfAbsent(id, TracedScheduledExecutorService.class, () -> {
                final String tracerRef = registry.find(Tracer.class).orElseThrow(() -> new NoSuchBeanDefinitionException(Tracer.class));
                return genericBeanDefinition(TracedScheduledExecutorService.class)
                        .addConstructorArgReference(executorId)
                        .addConstructorArgReference(tracerRef);
            });
        }

        return executorId;
    }

    private BeanMetadataElement registerRetryPolicy(final String id, final Client client) {
        if (client.getRetry().getEnabled()) {
            return ref(registry.registerIfAbsent(id, RetryPolicy.class, () ->
                    genericBeanDefinition(FailsafePluginFactory.class)
                            .setFactoryMethod("createRetryPolicy")
                            .addConstructorArgValue(client)));
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
                    .addConstructorArgReference(connectionManager)
                    .addConstructorArgValue(registry.findRef(id, HttpClientCustomizer.class).orElse(null))
                    .addConstructorArgValue(registry.findRef(id, HttpCacheStorage.class).orElse(null))
                    .setDestroyMethodName("close");
        });
    }

    private List<BeanMetadataElement> configureFirstRequestInterceptors(final String id, final Client client) {
        final List<BeanMetadataElement> interceptors = list();

        if (client.getTracing().getEnabled() && client.getTracing().getPropagateFlowId()) {
            log.debug("Client [{}]: Registering FlowHttpRequestInterceptor", id);
            interceptors.add(ref("flowHttpRequestInterceptor"));
        }

        return interceptors;
    }

}
