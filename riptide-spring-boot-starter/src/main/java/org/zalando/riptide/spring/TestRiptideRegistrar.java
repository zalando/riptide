package org.zalando.riptide.spring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.http.client.AsyncClientHttpRequestFactory;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

@Slf4j
@AllArgsConstructor
class TestRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach((id, client) ->
                registerAsyncClientHttpRequestFactory(id));
    }

    private void registerAsyncClientHttpRequestFactory(final String id) {
        registry.registerIfAbsent(id, AsyncClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering mocked AsyncClientHttpRequestFactory", id);
            final BeanDefinitionBuilder factory = genericBeanDefinition(AsyncClientHttpRequestFactory.class);
            factory.addDependsOn(RiptideTestAutoConfiguration.SERVER_BEAN_NAME);
            factory.setFactoryMethodOnBean("getAsyncRequestFactory", RiptideTestAutoConfiguration.TEMPLATE_BEAN_NAME);
            return factory;
        });
    }

}
