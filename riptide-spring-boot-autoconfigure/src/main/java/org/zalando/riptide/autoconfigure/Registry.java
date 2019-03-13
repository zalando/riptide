package org.zalando.riptide.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

final class Registry {

    private static final Logger LOG = LoggerFactory.getLogger(Registry.class);

    private final BeanDefinitionRegistry registry;

    Registry(final BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    public boolean isRegistered(final String id, final Class<?> type) {
        return isRegistered(generateBeanName(id, type));
    }

    public boolean isRegistered(final String name) {
        return registry.isBeanNameInUse(name);
    }

    public <T> String registerIfAbsent(final Class<T> type, final Supplier<BeanDefinitionBuilder> factory) {
        final String name = generateBeanName(type);

        if (isRegistered(name)) {
            LOG.debug("Bean [{}] is already registered, skipping it.", name);
            return name;
        }

        registry.registerBeanDefinition(name, factory.get().getBeanDefinition());

        return name;
    }

    public <T> String registerIfAbsent(final String id, final Class<T> type) {
        return registerIfAbsent(id, type, () -> genericBeanDefinition(type));
    }

    public <T> String registerIfAbsent(final String id, final Class<T> type,
            final Supplier<BeanDefinitionBuilder> factory) {
        return registerIfAbsent(id, generateBeanName(id, type), factory);
    }

    public <T> String registerIfAbsent(final String id, final String infix, final Class<T> type,
            final Supplier<BeanDefinitionBuilder> factory) {
        return registerIfAbsent(id, generateBeanName(id, infix, type), factory);
    }

    public String registerIfAbsent(final String id, final String name, final Supplier<BeanDefinitionBuilder> factory) {
        if (isRegistered(name)) {
            LOG.debug("Bean [{}] is already registered, skipping it.", name);
            return name;
        }

        final AbstractBeanDefinition definition = factory.get().getBeanDefinition();

        definition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, id));

        registry.registerBeanDefinition(name, definition);

        return name;
    }

    public static <T> String generateBeanName(final Class<T> type) {
        return UPPER_CAMEL.to(LOWER_CAMEL, type.getSimpleName());
    }

    public static <T> String generateBeanName(final String id, final Class<T> type) {
        final String suffix = type.getSimpleName();
        return generateBeanName(id, suffix);
    }

    public static <T> String generateBeanName(final String id, String infix, final Class<T> type) {
        final String suffix = type.getSimpleName();
        return generateBeanName(id, infix + suffix);
    }

    public static String generateBeanName(final String id, final String suffix) {
        return LOWER_HYPHEN.to(LOWER_CAMEL, id) + suffix;
    }

    public static BeanReference ref(final String beanName) {
        return new RuntimeBeanReference(beanName);
    }

    @SafeVarargs
    public static <T> List<T> list(final T... elements) {
        final ManagedList<T> list = new ManagedList<>();
        Collections.addAll(list, elements);
        return list;
    }

}
