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

    private RiptideSettings settings;
    private BiFunction<Registry, RiptideSettings, RiptideRegistrar> registrarFactory;

    RiptidePostProcessor(final BiFunction<Registry, RiptideSettings, RiptideRegistrar> registrarFactory) {
        this.registrarFactory = registrarFactory;
    }

    @Override
    public void setEnvironment(final Environment environment) {
        final Collection<SettingsParser> parsers = Lists.newArrayList(load(SettingsParser.class));
        this.settings = parse((ConfigurableEnvironment) environment, parsers);
    }

    // visible for testing
    RiptideSettings parse(final ConfigurableEnvironment environment, final Collection<SettingsParser> parsers) {
        final SettingsParser parser = parsers.stream()
                .filter(SettingsParser::isApplicable)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No applicable riptide settings parser available"));

        return parser.parse(environment);
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
