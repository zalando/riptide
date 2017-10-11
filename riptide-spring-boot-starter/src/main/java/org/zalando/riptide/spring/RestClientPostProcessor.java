package org.zalando.riptide.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import lombok.SneakyThrows;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriTemplateHandler;
import org.zalando.riptide.Http;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.exceptions.TemporaryExceptionPlugin;
import org.zalando.riptide.httpclient.RestAsyncClientHttpRequestFactory;
import org.zalando.riptide.spring.RestSettings.Client;
import org.zalando.riptide.spring.RestSettings.Defaults;
import org.zalando.riptide.spring.zmon.ZmonRequestInterceptor;
import org.zalando.riptide.spring.zmon.ZmonResponseInterceptor;
import org.zalando.riptide.stream.Streams;
import org.zalando.stups.oauth2.httpcomponents.AccessTokensRequestInterceptor;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.tracer.concurrent.TracingExecutors;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Executors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.spring.Registry.generateBeanName;
import static org.zalando.riptide.spring.Registry.list;
import static org.zalando.riptide.spring.Registry.ref;

public class RestClientPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(RestClientPostProcessor.class);

    private ConfigurableEnvironment environment;
    private Registry registry;
    private RestSettings settings;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry beanDefinitionRegistry) {
        this.registry = new Registry(beanDefinitionRegistry);

        final RestSettings settings = getSettings();
        final Defaults defaults = settings.getDefaults();

        settings.getClients().forEach((id, client) -> {
            final String factoryId = registerAsyncClientHttpRequestFactory(id, defaults, client);
            final String convertersId = registerHttpMessageConverters(id);
            final String baseUrl = client.getBaseUrl();

            registerHttp(id, client, factoryId, convertersId, baseUrl);
            registerRestTemplate(id, factoryId, convertersId, baseUrl);
            registerAsyncRestTemplate(id, factoryId, convertersId, baseUrl);
        });
    }

    @VisibleForTesting
    @SneakyThrows
    RestSettings getSettings() {
        if (settings == null) {
            final PropertiesConfigurationFactory<RestSettings> factory =
                    new PropertiesConfigurationFactory<>(RestSettings.class);

            factory.setTargetName("riptide");
            factory.setPropertySources(environment.getPropertySources());
            factory.setConversionService(environment.getConversionService());

            settings = factory.getObject();
        }
        return settings;
    }

    private String registerHttpMessageConverters(final String id) {
        return registry.register(id, HttpMessageConverters.class, () -> {
            final List<Object> list = list();

            LOG.debug("Client [{}]: Registering StringHttpMessageConverter", id);
            list.add(genericBeanDefinition(StringHttpMessageConverter.class)
                    .addPropertyValue("writeAcceptCharset", false)
                    .getBeanDefinition());

            final String objectMapperId = findObjectMapper(id);

            LOG.debug("Client [{}]: Registering MappingJackson2HttpMessageConverter referencing [{}]", id, objectMapperId);
            list.add(genericBeanDefinition(MappingJackson2HttpMessageConverter.class)
                    .addConstructorArgReference(objectMapperId)
                    .getBeanDefinition());

            LOG.debug("Client [{}]: Registering StreamConverter referencing [{}]", id, objectMapperId);
            list.add(genericBeanDefinition(Streams.class)
                    .setFactoryMethod("streamConverter")
                    .addConstructorArgReference(objectMapperId)
                    .getBeanDefinition());

            return BeanDefinitionBuilder.genericBeanDefinition(ClientHttpMessageConverters.class)
                    .addConstructorArgValue(list);
        });
    }

    private String findObjectMapper(final String id) {
        final String beanName = generateBeanName(id, ObjectMapper.class);
        return registry.isRegistered(beanName) ? beanName : "jacksonObjectMapper";
    }

    private String registerAccessTokens(final String id, final RestSettings settings) {
        return registry.register(AccessTokens.class, () -> {
            LOG.debug("Client [{}]: Registering AccessTokens", id);
            final BeanDefinitionBuilder builder = genericBeanDefinition(AccessTokensFactoryBean.class);
            builder.addPropertyValue("settings", settings);
            return builder;
        });
    }

    private String registerHttp(final String id, final Client client, final String factoryId,
            final String convertersId, @Nullable final String baseUrl) {
        return registry.register(id, Http.class, () -> {
            LOG.debug("Client [{}]: Registering Http", id);

            final BeanDefinitionBuilder http = genericBeanDefinition(HttpFactory.class);
            http.setFactoryMethod("create");
            http.addConstructorArgReference(factoryId);

            final AbstractBeanDefinition converters = BeanDefinitionBuilder.genericBeanDefinition()
                    .setFactoryMethod("getConverters")
                    .getBeanDefinition();
            converters.setFactoryBeanName(convertersId);

            http.addConstructorArgValue(converters);
            http.addConstructorArgValue(baseUrl);
            http.addConstructorArgValue(registerPlugins(settings.getDefaults(), client));

            return http;
        });
    }

    private List<Object> registerPlugins(final Defaults defaults, final Client client) {
        final List<Object> list = list();

        if (firstNonNull(client.getKeepOriginalStackTrace(), defaults.isKeepOriginalStackTrace())) {
            list.add(genericBeanDefinition(OriginalStackTracePlugin.class)
                    .getBeanDefinition());
        }

        if (firstNonNull(client.getDetectTransientFaults(), defaults.isDetectTransientFaults())) {
            list.add(ref(registry.register(TemporaryExceptionPlugin.class, () ->
                    genericBeanDefinition(TemporaryExceptionPlugin.class))));
        }

        return list;
    }

    @CanIgnoreReturnValue
    private String registerRestTemplate(final String id, final String factoryId, final String convertersId,
            @Nullable final String baseUrl) {
        return registry.register(id, RestTemplate.class, () -> {
            LOG.debug("Client [{}]: Registering RestTemplate", id);

            final BeanDefinitionBuilder restTemplate = genericBeanDefinition(RestTemplate.class);

            restTemplate.addConstructorArgReference(factoryId);

            final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
            handler.setBaseUrl(baseUrl);
            restTemplate.addPropertyValue("uriTemplateHandler", handler);

            final AbstractBeanDefinition converters = BeanDefinitionBuilder.genericBeanDefinition()
                    .setFactoryMethod("getConverters")
                    .getBeanDefinition();
            converters.setFactoryBeanName(convertersId);
            restTemplate.addPropertyValue("messageConverters", converters);

            return restTemplate;
        });
    }

    private String registerAsyncRestTemplate(final String id, final String factoryId, final String convertersId,
            @Nullable final String baseUrl) {
        return registry.register(id, AsyncRestTemplate.class, () -> {
            LOG.debug("Client [{}]: Registering AsyncRestTemplate", id);

            final BeanDefinitionBuilder restTemplate = genericBeanDefinition(AsyncRestTemplate.class);

            restTemplate.addConstructorArgReference(factoryId);

            final DefaultUriTemplateHandler handler = new DefaultUriTemplateHandler();
            handler.setBaseUrl(baseUrl);
            restTemplate.addPropertyValue("uriTemplateHandler", handler);

            final AbstractBeanDefinition converters = BeanDefinitionBuilder.genericBeanDefinition()
                    .setFactoryMethod("getConverters")
                    .getBeanDefinition();
            converters.setFactoryBeanName(convertersId);
            restTemplate.addPropertyValue("messageConverters", converters);

            return restTemplate;
        });
    }

    private String registerAsyncListenableTaskExecutor(final String id) {
        return registry.register(id, AsyncListenableTaskExecutor.class, () -> {
            LOG.debug("Client [{}]: Registering AsyncListenableTaskExecutor", id);

            return genericBeanDefinition(ConcurrentTaskExecutor.class)
                    .addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(TracingExecutors.class)
                            .setFactoryMethod("preserve")
                            .addConstructorArgValue(genericBeanDefinition(Executors.class)
                                    .setFactoryMethod("newCachedThreadPool")
                                    .setDestroyMethodName("shutdown")
                                    .getBeanDefinition())
                            .addConstructorArgReference("tracer")
                            .getBeanDefinition());
        });
    }

    private String registerAsyncClientHttpRequestFactory(final String id, final Defaults defaults,
            final Client client) {
        return registry.register(id, AsyncClientHttpRequestFactory.class, () -> {
            LOG.debug("Client [{}]: Registering RestAsyncClientHttpRequestFactory", id);

            final BeanDefinitionBuilder factory =
                    genericBeanDefinition(RestAsyncClientHttpRequestFactory.class);

            factory.addConstructorArgReference(registerHttpClient(id, defaults, client));
            factory.addConstructorArgReference(registerAsyncListenableTaskExecutor(id));

            return factory;
        });
    }

    private String registerHttpClient(final String id, final Defaults defaults, final Client client) {
        return registry.register(id, HttpClient.class, () -> {
            LOG.debug("Client [{}]: Registering HttpClient", id);

            final BeanDefinitionBuilder httpClient = genericBeanDefinition(HttpClientFactoryBean.class);
            configure(httpClient, id, defaults, client);
            configureInterceptors(httpClient, id, client);
            configureKeystore(httpClient, id, client.getKeystore());

            final String customizerId = generateBeanName(id, HttpClientCustomizer.class);
            if (registry.isRegistered(customizerId)) {
                LOG.debug("Client [{}]: Customizing HttpClient with [{}]", id, customizerId);
                httpClient.addPropertyReference("customizer", customizerId);
            }

            return httpClient;
        });
    }

    private void configure(final BeanDefinitionBuilder builder, final String id, final Defaults defaults,
            final Client client) {
        configure(builder, id, "connectionTimeout",
                firstNonNull(client.getConnectionTimeout(), defaults.getConnectionTimeout()));
        configure(builder, id, "socketTimeout",
                firstNonNull(client.getSocketTimeout(), defaults.getSocketTimeout()));
        configure(builder, id, "connectionTimeToLive",
                firstNonNull(client.getConnectionTimeToLive(), defaults.getConnectionTimeToLive()));

        final int maxConnectionsPerRoute =
                firstNonNull(client.getMaxConnectionsPerRoute(), defaults.getMaxConnectionsPerRoute());
        configure(builder, id, "maxConnectionsPerRoute", maxConnectionsPerRoute);

        final int maxConnectionsTotal = Math.max(
                maxConnectionsPerRoute,
                firstNonNull(client.getMaxConnectionsTotal(), defaults.getMaxConnectionsTotal()));

        configure(builder, id, "maxConnectionsTotal", maxConnectionsTotal);
    }

    private void configure(final BeanDefinitionBuilder bean, final String id, final String name, final Object value) {
        LOG.debug("Client [{}]: Configuring {}: [{}]", id, name, value);
        bean.addPropertyValue(name, value);
    }

    private void configureInterceptors(final BeanDefinitionBuilder builder, final String id,
            final Client client) {
        final List<Object> requestInterceptors = list();
        final List<Object> responseInterceptors = list();

        if (client.getOauth() != null) {
            LOG.debug("Client [{}]: Registering AccessTokensRequestInterceptor", id);
            requestInterceptors.add(genericBeanDefinition(AccessTokensRequestInterceptor.class)
                    .addConstructorArgValue(id)
                    .addConstructorArgReference(registerAccessTokens(id, getSettings()))
                    .getBeanDefinition());
        }

        LOG.debug("Client [{}]: Registering TracerHttpRequestInterceptor", id);
        requestInterceptors.add(ref("tracerHttpRequestInterceptor"));

        if (registry.isRegistered("zmonMetricsWrapper")) {
            LOG.debug("Client [{}]: Registering ZmonRequestInterceptor", id);
            requestInterceptors.add(genericBeanDefinition(ZmonRequestInterceptor.class).getBeanDefinition());
            LOG.debug("Client [{}]: Registering ZmonResponseInterceptor", id);
            responseInterceptors.add(genericBeanDefinition(ZmonResponseInterceptor.class)
                    .addConstructorArgValue(ref("zmonMetricsWrapper"))
                    .getBeanDefinition());
        }

        LOG.debug("Client [{}]: Registering LogbookHttpResponseInterceptor", id);
        responseInterceptors.add(ref("logbookHttpResponseInterceptor"));

        LOG.debug("Client [{}]: Registering LogbookHttpRequestInterceptor", id);
        final List<Object> lastRequestInterceptors = list(ref("logbookHttpRequestInterceptor"));

        if (client.isCompressRequest()) {
            LOG.debug("Client [{}]: Registering GzippingHttpRequestInterceptor", id);
            lastRequestInterceptors.add(new GzippingHttpRequestInterceptor());
        }

        builder.addPropertyValue("firstRequestInterceptors", requestInterceptors);
        builder.addPropertyValue("lastRequestInterceptors", lastRequestInterceptors);
        builder.addPropertyValue("lastResponseInterceptors", responseInterceptors);
    }

    private void configureKeystore(final BeanDefinitionBuilder httpClient, final String id, @Nullable final RestSettings.Keystore keystore) {
        if (keystore != null) {
            LOG.debug("Client [{}]: Registering trusted keystore", id);
            httpClient.addPropertyValue("trustedKeystore", keystore);
        }
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

}
