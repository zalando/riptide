package org.zalando.riptide.autoconfigure;

import org.springframework.beans.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.context.properties.source.*;
import org.springframework.context.*;
import org.springframework.core.env.*;

import java.util.function.*;

import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.*;

final class RiptidePostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private RiptideProperties properties;
    private BiFunction<Registry, RiptideProperties, RiptideRegistrar> registrarFactory;

    RiptidePostProcessor(final BiFunction<Registry, RiptideProperties, RiptideRegistrar> registrarFactory) {
        this.registrarFactory = registrarFactory;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        final Iterable<ConfigurationPropertySource> sources =
                from(((ConfigurableEnvironment) environment).getPropertySources());
        final Binder binder = new Binder(sources);

        this.properties = Defaulting.withDefaults(binder.bind("riptide", RiptideProperties.class).orElseCreate(RiptideProperties.class));
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        final RiptideRegistrar registrar = registrarFactory.apply(new Registry(registry), properties);
        registrar.register();
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        final DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, properties);
        configurer.register();
    }

}
