package org.zalando.riptide.spring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

@Slf4j
@AllArgsConstructor
class TestRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideSettings settings;

    @Override
    public void register() {
        final String templateId = registerMockAsyncRestTemplate();
        final String serverId = registerMockRestServiceServer(templateId);
        settings.getClients().forEach((id, client) -> registerAsyncClientHttpRequestFactory(id, templateId, serverId));
    }

    private String registerMockAsyncRestTemplate() {
        return registry.registerIfAbsent("_mock", AsyncRestTemplate.class, () -> {
            log.debug("Registering AsyncRestTemplate");
            return genericBeanDefinition(AsyncRestTemplate.class);
        });
    }

    private String registerMockRestServiceServer(final String templateId) {
        return registry.registerIfAbsent(MockRestServiceServer.class, () -> {
            log.debug("Registering MockRestServiceServer");
            final BeanDefinitionBuilder factory = genericBeanDefinition(MockRestServiceServer.class);
            factory.setFactoryMethod("createServer");
            factory.addConstructorArgReference(templateId);
            return factory;
        });
    }

    private void registerAsyncClientHttpRequestFactory(final String id, final String templateId, final String serverId) {
        registry.registerIfAbsent(id, AsyncClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering mocked AsyncClientHttpRequestFactory", id);
            final BeanDefinitionBuilder factory = genericBeanDefinition(AsyncClientHttpRequestFactory.class);
            factory.addDependsOn(serverId);
            factory.setFactoryMethodOnBean("getAsyncRequestFactory", templateId);
            return factory;
        });
    }

}
