package org.zalando.riptide.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.ConnectionClosedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.PluginInterceptor;
import org.zalando.riptide.backup.BackupRequestPlugin;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.faults.FaultClassifier;
import org.zalando.riptide.faults.TransientFaultPlugin;
import org.zalando.riptide.httpclient.GzipHttpRequestInterceptor;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.spring.RiptideProperties.Client;
import org.zalando.riptide.spring.RiptideProperties.Client.Keystore;
import org.zalando.riptide.stream.Streams;
import org.zalando.riptide.timeout.TimeoutPlugin;
import org.zalando.stups.oauth2.httpcomponents.AccessTokensRequestInterceptor;
import org.zalando.stups.tokens.AccessTokens;
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

import static java.util.stream.Collectors.toCollection;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.spring.Dependencies.ifPresent;
import static org.zalando.riptide.spring.Registry.generateBeanName;
import static org.zalando.riptide.spring.Registry.list;
import static org.zalando.riptide.spring.Registry.ref;

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
            final String baseUrl = client.getBaseUrl();
            final List<String> plugins = registerPlugins(id, client);

            registerHttp(id, client, factoryId, converters, plugins);
            registerTemplate(id, RestTemplate.class, factoryId, baseUrl, converters, plugins);
            registerTemplate(id, AsyncRestTemplate.class, factoryId, baseUrl, converters, plugins);
        });
    }

    private String registerAsyncClientHttpRequestFactory(final String id, final Client client) {
        return registry.registerIfAbsent(id, AsyncClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);

            final BeanDefinitionBuilder factory =
                    genericBeanDefinition(RestAsyncClientHttpRequestFactory.class);

            factory.addConstructorArgReference(registerHttpClient(id, client));
            factory.addConstructorArgValue(genericBeanDefinition(ConcurrentTaskExecutor.class)
                    // we allow users to use their own ExecutorService, but they don't have to configure tracing
                    .addConstructorArgValue(registerExecutor(id, client))
                    .getBeanDefinition());

            return factory;
        });
    }

    private BeanMetadataElement registerExecutor(final String id, final Client client) {
        return trace(registry.registerIfAbsent(id, ExecutorService.class, () -> {
            final RiptideProperties.ThreadPool threadPool = client.getThreadPool();
            return genericBeanDefinition(ThreadPoolExecutor.class)
                    .addConstructorArgValue(threadPool.getMinSize())
                    .addConstructorArgValue(threadPool.getMaxSize())
                    .addConstructorArgValue(threadPool.getKeepAlive().getAmount())
                    .addConstructorArgValue(threadPool.getKeepAlive().getUnit())
                    .addConstructorArgValue(threadPool.getQueueSize() == 0 ?
                            new SynchronousQueue<>() :
                            new ArrayBlockingQueue<>(threadPool.getQueueSize()))
                    .addConstructorArgValue(new CustomizableThreadFactory("http-" + id + "-"))
                    .setDestroyMethodName("shutdown");
        }));
    }

    private static final class HttpMessageConverters {

    }

    private BeanDefinition registerHttpMessageConverters(final String id) {
        // we use the wrong type here since that's the easiest way to influence the name
        // we want exampleHttpMessageConverters, rather than exampleClientHttpMessageConverters

        final String convertersId = registry.registerIfAbsent(id, HttpMessageConverters.class, () -> {
            final List<Object> list = list();

            log.debug("Client [{}]: Registering StringHttpMessageConverter", id);
            list.add(genericBeanDefinition(StringHttpMessageConverter.class)
                    .addPropertyValue("writeAcceptCharset", false)
                    .getBeanDefinition());

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

            return genericBeanDefinition(ClientHttpMessageConverters.class)
                    .addConstructorArgValue(list);
        });

        final AbstractBeanDefinition converters = genericBeanDefinition()
                .setFactoryMethod("getConverters")
                .getBeanDefinition();
        converters.setFactoryBeanName(convertersId);

        return converters;
    }

    private void registerHttp(final String id, final Client client, final String factoryId,
            final BeanDefinition converters, final List<String> plugins) {
        registry.registerIfAbsent(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            final BeanDefinitionBuilder http = genericBeanDefinition(HttpFactory.class);
            http.setFactoryMethod("create");
            http.addConstructorArgValue(client.getBaseUrl());
            http.addConstructorArgValue(client.getUrlResolution());
            http.addConstructorArgReference(factoryId);
            http.addConstructorArgValue(converters);
            http.addConstructorArgValue(plugins.stream()
                    .map(Registry::ref)
                    .collect(toCollection(Registry::list)));

            return http;
        });
    }

    private void registerTemplate(final String id, final Class<?> type, final String factoryId,
            @Nullable final String baseUrl, final BeanDefinition converters, final List<String> plugins) {
        registry.registerIfAbsent(id, type, () -> {
            log.debug("Client [{}]: Registering AsyncRestTemplate", id);

            final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
            handler.setBaseUrl(baseUrl);

            final BeanDefinitionBuilder template = genericBeanDefinition(type);
            template.addConstructorArgReference(factoryId);
            template.addPropertyValue("uriTemplateHandler", handler);
            template.addPropertyValue("messageConverters", converters);
            template.addPropertyValue("interceptors", plugins.stream()
                    .map(plugin -> genericBeanDefinition(PluginInterceptor.class)
                            .addConstructorArgReference(plugin)
                            .getBeanDefinition())
                    .collect(toCollection(Registry::list)));

            return template;
        });
    }

    private String findObjectMapper(final String id) {
        final String beanName = generateBeanName(id, ObjectMapper.class);
        return registry.isRegistered(beanName) ? beanName : "jacksonObjectMapper";
    }

    private String registerAccessTokens(final String id, final RiptideProperties settings) {
        return registry.registerIfAbsent(AccessTokens.class, () -> {
            log.debug("Client [{}]: Registering AccessTokens", id);
            final BeanDefinitionBuilder builder = genericBeanDefinition(AccessTokensFactoryBean.class);
            builder.addConstructorArgValue(settings);
            return builder;
        });
    }

    private List<String> registerPlugins(final String id, final Client client) {
        final List<String> plugins = list();

        if (client.getRecordMetrics()) {
            log.debug("Client [{}]: Registering [{}]", id, MetricsPlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, MetricsPlugin.class, () ->
                    genericBeanDefinition(Metrics.class)
                            .setFactoryMethod("createMetricsPlugin")
                            .addConstructorArgReference("meterRegistry")
                            .addConstructorArgValue(ImmutableList.of(clientId(id)))));
        }

        if (client.getDetectTransientFaults()) {
            log.debug("Client [{}]: Registering [{}]", id, TransientFaultPlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, TransientFaultPlugin.class, () ->
                    genericBeanDefinition(TransientFaultPlugin.class)
                            .addConstructorArgReference(findFaultClassifier(id))));
        }

        if (client.getRetry() != null || client.getCircuitBreaker() != null) {
            log.debug("Client [{}]: Registering [{}]", id, FailsafePlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, FailsafePlugin.class, () ->
                    genericBeanDefinition(FailsafePlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgReference(registerRetryPolicy(id, client))
                            .addConstructorArgReference(registerCircuitBreaker(id, client))
                            .addConstructorArgReference(registerRetryListener(id, client))));
        }

        if (client.getBackupRequest() != null) {
            log.debug("Client [{}]: Registering [{}]", id, BackupRequestPlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, BackupRequestPlugin.class, () ->
                    genericBeanDefinition(BackupRequestPlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getAmount())
                            .addConstructorArgValue(client.getBackupRequest().getDelay().getUnit())
                            .addConstructorArgValue(registerExecutor(id, client))));
        }

        if (client.getTimeout() != null) {
            log.debug("Client [{}]: Registering [{}]", id, TimeoutPlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, TimeoutPlugin.class, () ->
                    genericBeanDefinition(TimeoutPlugin.class)
                            .addConstructorArgValue(registerScheduler(id, client))
                            .addConstructorArgValue(client.getTimeout().getAmount())
                            .addConstructorArgValue(client.getTimeout().getUnit())
                            .addConstructorArgValue(registerExecutor(id, client))));
        }

        if (client.getPreserveStackTrace()) {
            log.debug("Client [{}]: Registering [{}]", id, OriginalStackTracePlugin.class.getSimpleName());
            plugins.add(registry.registerIfAbsent(id, OriginalStackTracePlugin.class));
        }

        if (registry.isRegistered(id, Plugin.class)) {
            final String plugin = generateBeanName(id, Plugin.class);
            log.debug("Client [{}]: Registering [{}]", plugin);
            plugins.add(plugin);
        }

        return plugins;
    }

    private String findFaultClassifier(final String id) {
        if (registry.isRegistered(id, FaultClassifier.class)) {
            return generateBeanName(id, FaultClassifier.class);
        } else if (registry.isRegistered(FaultClassifier.class)) {
            return generateBeanName(FaultClassifier.class);
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
        return trace(registry.registerIfAbsent(id, ScheduledExecutorService.class,
                () -> {
                    final CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(
                            "http-" + id + "-scheduler-");
                    threadFactory.setDaemon(true);

                    return genericBeanDefinition(ScheduledThreadPoolExecutor.class)
                            .addConstructorArgValue(client.getThreadPool().getMaxSize())
                            .addConstructorArgValue(threadFactory)
                            .addPropertyValue("removeOnCancelPolicy", true)
                            .setDestroyMethodName("shutdown");
                }));
    }

    private String registerRetryPolicy(final String id, final Client client) {
        return registry.registerIfAbsent(id, RetryPolicy.class, () -> {
            final BeanDefinitionBuilder retryPolicy = genericBeanDefinition(RetryPolicyFactoryBean.class);
            @Nullable final RiptideProperties.Retry retry = client.getRetry();
            if (retry != null) {
                retryPolicy.addPropertyValue("configuration", retry);
            }
            return retryPolicy;
        });
    }

    private String registerCircuitBreaker(final String id, final Client client) {
        return registry.registerIfAbsent(id, CircuitBreaker.class, () -> {
            final BeanDefinitionBuilder circuitBreaker = genericBeanDefinition(CircuitBreakerFactoryBean.class);
            @Nullable final RiptideProperties.CircuitBreaker breaker = client.getCircuitBreaker();
            if (client.getTimeout() != null) {
                circuitBreaker.addPropertyValue("timeout", client.getTimeout());
            }
            if (breaker != null) {
                circuitBreaker.addPropertyValue("configuration", breaker);
                circuitBreaker.addPropertyReference("listener",
                        registry.registerIfAbsent(id, CircuitBreakerListener.class, () -> {
                            if (client.getRecordMetrics()) {
                                return genericBeanDefinition(Metrics.class)
                                        .setFactoryMethod("createCircuitBreakerListener")
                                        .addConstructorArgReference("meterRegistry")
                                        .addConstructorArgValue(ImmutableList.of(clientId(id), clientName(id, client)));
                            } else {
                                return genericBeanDefinition(Metrics.class)
                                        .setFactoryMethod("getDefaultCircuitBreakerListener");
                            }
                        }));
            }

            return circuitBreaker;
        });
    }

    private String registerRetryListener(final String id, final Client client) {
        return registry.registerIfAbsent(id, RetryListener.class, () -> {
            if (client.getRecordMetrics()) {
                return genericBeanDefinition(Metrics.class)
                        .setFactoryMethod("createRetryListener")
                        .addConstructorArgReference("meterRegistry")
                        .addConstructorArgValue(ImmutableList.of(clientId(id)));
            } else {
                return genericBeanDefinition(Metrics.class)
                        .setFactoryMethod("getDefaultRetryListener");
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

    private BeanMetadataElement trace(final String executor) {
        if (registry.isRegistered("tracer")) {
            return genericBeanDefinition(TracingExecutors.class)
                    .setFactoryMethod("preserve")
                    .addConstructorArgReference(executor)
                    .addConstructorArgReference("tracer")
                    .getBeanDefinition();
        } else {
            return ref(executor);
        }
    }

    private String registerHttpClient(final String id, final Client client) {
        return registry.registerIfAbsent(id, HttpClient.class, () -> {
            log.debug("Client [{}]: Registering HttpClient", id);

            final BeanDefinitionBuilder httpClient = genericBeanDefinition(HttpClientFactoryBean.class);

            configure(httpClient, id, "connectTimeout", client.getConnectTimeout());
            configure(httpClient, id, "socketTimeout", client.getSocketTimeout());
            configure(httpClient, id, "connectionTimeToLive", client.getConnectionTimeToLive());
            configure(httpClient, id, "maxConnectionsPerRoute", client.getMaxConnectionsPerRoute());
            configure(httpClient, id, "maxConnectionsTotal", client.getMaxConnectionsTotal());

            configureInterceptors(httpClient, id, client);
            configureKeystore(httpClient, id, client.getKeystore());

            final String customizerId = generateBeanName(id, HttpClientCustomizer.class);
            if (registry.isRegistered(customizerId)) {
                log.debug("Client [{}]: Customizing HttpClient with [{}]", id, customizerId);
                httpClient.addPropertyReference("customizer", customizerId);
            }

            httpClient.setDestroyMethodName("destroy");

            return httpClient;
        });
    }

    private void configure(final BeanDefinitionBuilder bean, final String id, final String name, final Object value) {
        log.debug("Client [{}]: Configuring {}: [{}]", id, name, value);
        bean.addPropertyValue(name, value);
    }

    private void configureInterceptors(final BeanDefinitionBuilder builder, final String id,
            final Client client) {
        final List<Object> requestInterceptors = list();
        final List<Object> responseInterceptors = list();

        if (client.getOauth() != null) {
            log.debug("Client [{}]: Registering AccessTokensRequestInterceptor", id);
            requestInterceptors.add(genericBeanDefinition(AccessTokensRequestInterceptor.class)
                    .addConstructorArgValue(id)
                    .addConstructorArgReference(registerAccessTokens(id, properties))
                    .getBeanDefinition());
        }

        if (registry.isRegistered("tracerHttpRequestInterceptor")) {
            log.debug("Client [{}]: Registering TracerHttpRequestInterceptor", id);
            requestInterceptors.add(ref("tracerHttpRequestInterceptor"));
        }

        if (registry.isRegistered("logbookHttpResponseInterceptor")) {
            log.debug("Client [{}]: Registering LogbookHttpResponseInterceptor", id);
            responseInterceptors.add(ref("logbookHttpResponseInterceptor"));
        }

        final List<Object> lastRequestInterceptors = list();

        if (registry.isRegistered("logbookHttpRequestInterceptor")) {
            log.debug("Client [{}]: Registering LogbookHttpRequestInterceptor", id);
            lastRequestInterceptors.add(ref("logbookHttpRequestInterceptor"));
        }

        if (client.isCompressRequest()) {
            log.debug("Client [{}]: Registering GzippingHttpRequestInterceptor", id);
            lastRequestInterceptors.add(new GzipHttpRequestInterceptor());
        }

        builder.addPropertyValue("firstRequestInterceptors", requestInterceptors);
        builder.addPropertyValue("lastRequestInterceptors", lastRequestInterceptors);
        builder.addPropertyValue("lastResponseInterceptors", responseInterceptors);
    }

    private void configureKeystore(final BeanDefinitionBuilder httpClient, final String id,
            @Nullable final Keystore keystore) {
        if (keystore == null) {
            return;
        }

        log.debug("Client [{}]: Registering trusted keystore", id);
        httpClient.addPropertyValue("trustedKeystore", keystore);
    }

}
