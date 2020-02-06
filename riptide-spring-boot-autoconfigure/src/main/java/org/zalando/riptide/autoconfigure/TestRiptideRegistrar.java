package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.zalando.riptide.auth.AuthorizationProvider;
import org.zalando.riptide.autoconfigure.RiptideProperties.Client;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.zalando.riptide.autoconfigure.RiptideTestAutoConfiguration.REST_TEMPLATE_BEAN_NAME;
import static org.zalando.riptide.autoconfigure.RiptideTestAutoConfiguration.SERVER_BEAN_NAME;

@Slf4j
@AllArgsConstructor
class TestRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach(this::register);
    }

    private void register(final String id, final Client client) {
        registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering mocked ClientHttpRequestFactory", id);
            final BeanDefinitionBuilder factory = genericBeanDefinition(ClientHttpRequestFactory.class);
            factory.addDependsOn(SERVER_BEAN_NAME);
            factory.setFactoryMethodOnBean("getRequestFactory", REST_TEMPLATE_BEAN_NAME);
            return factory;
        });

        if (client.getAuth().getEnabled()) {
            registry.registerIfAbsent(id, AuthorizationProvider.class, () ->
                    genericBeanDefinition(MockAuthorizationProvider.class));
        }
    }

}
