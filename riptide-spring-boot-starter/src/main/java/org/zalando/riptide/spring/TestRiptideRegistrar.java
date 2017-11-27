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
        settings.getClients().forEach((id, client) -> {
            final String templateId = registerAsyncRestTemplate(id);
            final String serverId = registerMockRestServiceServer(id, templateId);
            registerAsyncClientHttpRequestFactory(id, templateId, serverId);
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

    private String registerAsyncRestTemplate(final String id) {
        return registry.registerIfAbsent(id, AsyncRestTemplate.class, () -> {
            log.debug("Client [{}]: Registering AsyncRestTemplate", id);
            return genericBeanDefinition(AsyncRestTemplate.class);
        });
    }

    private String registerMockRestServiceServer(final String id, final String templateId) {
        return registry.registerIfAbsent(id, MockRestServiceServer.class, () -> {
            log.debug("Client [{}]: Registering MockRestServiceServer", id);
            final BeanDefinitionBuilder factory = genericBeanDefinition(MockRestServiceServer.class);
            factory.setFactoryMethod("createServer");
            factory.addConstructorArgReference(templateId);
            return factory;
        });
    }

}
