package org.zalando.riptide.spring;

import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import static org.zalando.riptide.spring.Defaulting.withDefaults;

final class RiptidePostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private RiptideSettings settings;

    @Override
    @SneakyThrows
    public void setEnvironment(final Environment environment) {
        final ConfigurableEnvironment environment1 = (ConfigurableEnvironment) environment;

        final PropertiesConfigurationFactory<RiptideSettings> factory =
                new PropertiesConfigurationFactory<>(RiptideSettings.class);

        factory.setTargetName("riptide");
        factory.setPropertySources(environment1.getPropertySources());
        factory.setConversionService(environment1.getConversionService());

        this.settings = factory.getObject();
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        final RiptideRegistrar registrar = new RiptideRegistrar(new Registry(registry), withDefaults(settings));
        registrar.register();
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

}
