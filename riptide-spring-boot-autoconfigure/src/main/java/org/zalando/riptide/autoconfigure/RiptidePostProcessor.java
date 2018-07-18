package org.zalando.riptide.autoconfigure;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.function.BiFunction;

import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;

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

        this.properties = binder.bind("riptide", RiptideProperties.class).orElseCreate(RiptideProperties.class);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        final RiptideRegistrar registrar = registrarFactory.apply(new Registry(registry), Defaulting.withDefaults(properties));
        registrar.register();
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

}
