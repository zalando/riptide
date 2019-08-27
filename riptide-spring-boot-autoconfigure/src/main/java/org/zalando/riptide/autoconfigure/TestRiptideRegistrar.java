package org.zalando.riptide.autoconfigure;

import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.support.*;
import org.springframework.http.client.*;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;
import static org.zalando.riptide.autoconfigure.RiptideTestAutoConfiguration.*;

@Slf4j
@AllArgsConstructor
class TestRiptideRegistrar implements RiptideRegistrar {

    private final Registry registry;
    private final RiptideProperties properties;

    @Override
    public void register() {
        properties.getClients().forEach((id, client) ->
                registerRequestFactories(id));
    }

    private void registerRequestFactories(final String id) {
        registry.registerIfAbsent(id, ClientHttpRequestFactory.class, () -> {
            log.debug("Client [{}]: Registering mocked ClientHttpRequestFactory", id);
            final BeanDefinitionBuilder factory = genericBeanDefinition(ClientHttpRequestFactory.class);
            factory.addDependsOn(SERVER_BEAN_NAME);
            factory.setFactoryMethodOnBean("getRequestFactory", REST_TEMPLATE_BEAN_NAME);
            return factory;
        });
    }

}
