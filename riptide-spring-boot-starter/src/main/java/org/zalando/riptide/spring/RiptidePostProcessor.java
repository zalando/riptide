package org.zalando.riptide.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.util.function.BiFunction;

import static org.zalando.riptide.spring.Defaulting.withDefaults;

final class RiptidePostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private RiptideSettings settings;
    private BiFunction<Registry, RiptideSettings, RiptideRegistrar> registrarFactory;

    RiptidePostProcessor(final BiFunction<Registry, RiptideSettings, RiptideRegistrar> registrarFactory) {
        this.registrarFactory = registrarFactory;
    }

    @Override
    public void setEnvironment(final Environment env) {
        final ConfigurableEnvironment environment = (ConfigurableEnvironment) env;

        final MutablePropertySources propertySources = environment.getPropertySources();
        final Iterable<ConfigurationPropertySource> from = ConfigurationPropertySources.from(propertySources);
        final Binder binder = new Binder(from, null, environment.getConversionService());

        this.settings = binder.bind("riptide", RiptideSettings.class).orElseCreate(RiptideSettings.class);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        final RiptideRegistrar registrar = registrarFactory.apply(new Registry(registry), withDefaults(settings));
        registrar.register();
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

}
