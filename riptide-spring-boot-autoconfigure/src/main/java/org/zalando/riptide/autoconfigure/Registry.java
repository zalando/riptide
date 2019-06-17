package org.zalando.riptide.autoconfigure;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.zalando.riptide.autoconfigure.Name.name;

@AllArgsConstructor
@Slf4j
final class Registry {

    private final BeanDefinitionRegistry registry;
    private final ConfigurableListableBeanFactory beanFactory;

    boolean isRegistered(final String name) {
        return registry.isBeanNameInUse(name);
    }

    String registerIfAbsent(final String id, final Class<?> suffix, final Supplier<BeanDefinitionBuilder> factory) {
        return registerIfAbsent(name(id, suffix), factory);
    }

    String registerIfAbsent(final Name name, final Supplier<BeanDefinitionBuilder> factory) {
        return find(name).orElseGet(() -> {
            final AbstractBeanDefinition definition = factory.get().getBeanDefinition();

            name.getId().ifPresent(id ->
                    definition.addQualifier(new AutowireCandidateQualifier(Qualifier.class, id)));

            final String beanName = name.toNormalizedString();
            log.debug("Registering [{}]", beanName);
            registry.registerBeanDefinition(beanName, definition);
            return beanName;
        });
    }

    Optional<BeanReference> findRef(final String id, final Class<?>... types) {
        return find(id, types).map(Registry::ref);
    }

    Optional<String> find(final String id, final Class<?>... types) {
        return find(name(id, types));
    }

    Optional<String> find(Class<?> type) {
        return Stream.of(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type))
              .findFirst();
    }

    private Optional<String> find(final Name name) {
        return name.getAlternatives().stream()
                .filter(this::isRegistered)
                .findFirst();
    }

    static BeanReference ref(final String beanName) {
        return new RuntimeBeanReference(beanName);
    }

    @SafeVarargs
    public static <T> List<T> list(final T... elements) {
        final ManagedList<T> list = new ManagedList<>();
        Collections.addAll(list, elements);
        return list;
    }

}
