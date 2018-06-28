package org.zalando.riptide.spring;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import static java.util.ServiceLoader.load;
import static org.zalando.riptide.spring.Defaulting.withDefaults;

final class RiptidePostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private RiptideProperties properties;
    private BiFunction<Registry, RiptideProperties, RiptideRegistrar> registrarFactory;

    RiptidePostProcessor(final BiFunction<Registry, RiptideProperties, RiptideRegistrar> registrarFactory) {
        this.registrarFactory = registrarFactory;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        final Collection<SettingsParser> parsers = Lists.newArrayList(load(SettingsParser.class));
        this.properties = parse((ConfigurableEnvironment) environment, parsers);
    }

    // visible for testing
    RiptideProperties parse(final ConfigurableEnvironment environment, final Collection<SettingsParser> parsers) {
        final SettingsParser parser = parsers.stream()
                .filter(SettingsParser::isApplicable)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No applicable riptide settings parser available"));

        return parser.parse(environment);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) {
        final RiptideRegistrar registrar = registrarFactory.apply(new Registry(registry), withDefaults(properties));
        registrar.register();
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // nothing to do
    }

}
