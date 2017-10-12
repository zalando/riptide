package org.zalando.riptide.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.RetryPolicy;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.core.task.AsyncListenableTaskExecutor;
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
import org.zalando.riptide.exceptions.TemporaryExceptionPlugin;
import org.zalando.riptide.failsafe.FailsafePlugin;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;
import org.zalando.riptide.spring.RiptideSettings.Client;
import org.zalando.riptide.spring.RiptideSettings.Client.Keystore;
import org.zalando.riptide.spring.RiptideSettings.Failsafe;
import org.zalando.riptide.spring.zmon.ZmonRequestInterceptor;
import org.zalando.riptide.spring.zmon.ZmonResponseInterceptor;
import org.zalando.riptide.stream.Streams;
import org.zalando.stups.oauth2.httpcomponents.AccessTokensRequestInterceptor;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.tracer.concurrent.TracingExecutors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.spring.Registry.generateBeanName;
import static org.zalando.riptide.spring.Registry.list;
import static org.zalando.riptide.spring.Registry.ref;

@Slf4j
@AllArgsConstructor
final class RiptideRegistrar {

    private final Registry registry;
    private final RiptideSettings settings;

    public void register() {
        settings.getClients().forEach((id, client) -> {
            final String factoryId = registerAsyncClientHttpRequestFactory(id, client);
            final BeanDefinition converters = registerHttpMessageConverters(id);
            final String baseUrl = client.getBaseUrl();

            registerHttp(id, client, factoryId, converters);
            registerTemplate(id, RestTemplate.class, factoryId, converters, baseUrl);
            registerTemplate(id, AsyncRestTemplate.class, factoryId, converters, baseUrl);
        });
    }

    private String registerAsyncClientHttpRequestFactory(final String id, final Client client) {
        return registry.register(id, AsyncClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);

            final BeanDefinitionBuilder factory =
                    genericBeanDefinition(RestAsyncClientHttpRequestFactory.class);

            factory.addConstructorArgReference(registerHttpClient(id, client));
            factory.addConstructorArgReference(registerAsyncListenableTaskExecutor(id, client));

            return factory;
        });
    }

    private BeanDefinition registerHttpMessageConverters(final String id) {
        final String convertersId = registry.register(id, HttpMessageConverters.class, () -> {
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

            log.debug("Client [{}]: Registering StreamConverter referencing [{}]", id, objectMapperId);
            list.add(genericBeanDefinition(Streams.class)
                    .setFactoryMethod("streamConverter")
                    .addConstructorArgReference(objectMapperId)
                    .getBeanDefinition());

            return BeanDefinitionBuilder.genericBeanDefinition(ClientHttpMessageConverters.class)
                    .addConstructorArgValue(list);
        });

        final AbstractBeanDefinition converters = BeanDefinitionBuilder.genericBeanDefinition()
                .setFactoryMethod("getConverters")
                .getBeanDefinition();
        converters.setFactoryBeanName(convertersId);

        return converters;
    }

    private void registerHttp(final String id, final Client client, final String factoryId,
            final BeanDefinition converters) {
        registry.register(id, Http.class, () -> {
            log.debug("Client [{}]: Registering Http", id);

            final BeanDefinitionBuilder http = genericBeanDefinition(HttpFactory.class);
            http.setFactoryMethod("create");
            http.addConstructorArgReference(factoryId);
            http.addConstructorArgValue(converters);
            http.addConstructorArgValue(client.getBaseUrl());
            http.addConstructorArgValue(registerPlugins(id, client));

            return http;
        });
    }

    private void registerTemplate(final String id, final Class<?> type, final String factoryId,
            final BeanDefinition converters, @Nullable final String baseUrl) {
        registry.register(id, type, () -> {
            log.debug("Client [{}]: Registering AsyncRestTemplate", id);

            final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
            handler.setBaseUrl(baseUrl);

            final BeanDefinitionBuilder template = genericBeanDefinition(type);
            template.addConstructorArgReference(factoryId);
            template.addPropertyValue("uriTemplateHandler", handler);
            template.addPropertyValue("messageConverters", converters);

            return template;
        });
    }

    private String findObjectMapper(final String id) {
        final String beanName = generateBeanName(id, ObjectMapper.class);
        return registry.isRegistered(beanName) ? beanName : "jacksonObjectMapper";
    }

    private String registerAccessTokens(final String id, final RiptideSettings settings) {
        return registry.register(AccessTokens.class, () -> {
            log.debug("Client [{}]: Registering AccessTokens", id);
            final BeanDefinitionBuilder builder = genericBeanDefinition(AccessTokensFactoryBean.class);
            builder.addConstructorArgValue(settings);
            return builder;
        });
    }

    private List<Object> registerPlugins(final String id, final Client client) {
        final List<Object> plugins = list();

        if (client.getKeepOriginalStackTrace()) {
            log.debug("Client [{}]: Registering [{}]", id, OriginalStackTracePlugin.class.getSimpleName());
            plugins.add(ref(registry.register(id, OriginalStackTracePlugin.class)));
        }

        if (client.getDetectTransientFaults()) {
            log.debug("Client [{}]: Registering [{}]", id, TemporaryExceptionPlugin.class.getSimpleName());
            plugins.add(ref(registry.register(id, TemporaryExceptionPlugin.class)));
        }

        if (client.getFailsafe() != null) {
            plugins.add(ref(registry.register(id, FailsafePlugin.class, () -> {
                final BeanDefinitionBuilder plugin = genericBeanDefinition(FailsafePlugin.class);

                plugin.addConstructorArgReference(registerThreadPool(id, client));

                plugin.addConstructorArgReference(registry.register(id, RetryPolicy.class, () -> {
                    final BeanDefinitionBuilder retryPolicy = genericBeanDefinition(RetryPolicyFactoryBean.class);
                    @Nullable final Failsafe.Retry retry = client.getFailsafe().getRetry();
                    if (retry != null) {
                        retryPolicy.addPropertyValue("configuration", retry);
                    }
                    return retryPolicy;
                }));


                plugin.addConstructorArgReference(registry.register(id, CircuitBreaker.class, () -> {
                    final BeanDefinitionBuilder circuitBreaker = genericBeanDefinition(CircuitBreakerFactoryBean.class);
                    @Nullable final Failsafe.CircuitBreaker breaker = client.getFailsafe().getCircuitBreaker();
                    if (breaker != null) {
                        circuitBreaker.addPropertyValue("configuration", breaker);
                    }
                    return circuitBreaker;
                }));

                return plugin;
            })));
        }

        return plugins;
    }

    private String registerAsyncListenableTaskExecutor(final String id, final Client client) {
        return registry.register(id, AsyncListenableTaskExecutor.class, () ->
                genericBeanDefinition(ConcurrentTaskExecutor.class)
                        .addConstructorArgReference(registerThreadPool(id, client)));
    }

    private String registerThreadPool(final String id, final Client client) {
        return registry.register(id, ScheduledExecutorService.class, () ->
                genericBeanDefinition(TracingExecutors.class)
                        .setFactoryMethod("preserve")
                        .addConstructorArgValue(genericBeanDefinition(Executors.class)
                                .setFactoryMethod("newScheduledThreadPool")
                                // TODO should we have some breathing room for retries?
                                .addConstructorArgValue(client.getMaxConnectionsTotal())
                                .addConstructorArgValue(genericBeanDefinition(CustomizableThreadFactory.class)
                                        .addConstructorArgValue("http-" + id + "-")
                                        .getBeanDefinition())
                                .setDestroyMethodName("shutdown")
                                .getBeanDefinition())
                        .addConstructorArgReference("tracer"));
    }

    private String registerHttpClient(final String id, final Client client) {
        return registry.register(id, HttpClient.class, () -> {
            log.debug("Client [{}]: Registering HttpClient", id);

            final BeanDefinitionBuilder httpClient = genericBeanDefinition(HttpClientFactoryBean.class);

            configure(httpClient, id, "connectionTimeout", client.getConnectionTimeout());
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
                    .addConstructorArgReference(registerAccessTokens(id, settings))
                    .getBeanDefinition());
        }

        log.debug("Client [{}]: Registering TracerHttpRequestInterceptor", id);
        requestInterceptors.add(ref("tracerHttpRequestInterceptor"));

        if (registry.isRegistered("zmonMetricsWrapper")) {
            log.debug("Client [{}]: Registering ZmonRequestInterceptor", id);
            requestInterceptors.add(genericBeanDefinition(ZmonRequestInterceptor.class).getBeanDefinition());
            log.debug("Client [{}]: Registering ZmonResponseInterceptor", id);
            responseInterceptors.add(genericBeanDefinition(ZmonResponseInterceptor.class)
                    .addConstructorArgValue(ref("zmonMetricsWrapper"))
                    .getBeanDefinition());
        }

        log.debug("Client [{}]: Registering LogbookHttpResponseInterceptor", id);
        responseInterceptors.add(ref("logbookHttpResponseInterceptor"));

        log.debug("Client [{}]: Registering LogbookHttpRequestInterceptor", id);
        final List<Object> lastRequestInterceptors = list(ref("logbookHttpRequestInterceptor"));

        if (client.isCompressRequest()) {
            log.debug("Client [{}]: Registering GzippingHttpRequestInterceptor", id);
            lastRequestInterceptors.add(new GzippingHttpRequestInterceptor());
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
