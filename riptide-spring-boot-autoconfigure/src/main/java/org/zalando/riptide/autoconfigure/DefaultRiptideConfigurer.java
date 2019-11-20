package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.zalando.logbook.Logbook;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.zalando.riptide.autoconfigure.ValueConstants.LOGBOOK_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.METER_REGISTRY_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.TRACER_REF;

@AllArgsConstructor
class DefaultRiptideConfigurer {
    private final ConfigurableListableBeanFactory beanFactory;
    private final RiptideProperties properties;

    void register() {
        final Map<String, BeanMetadataElement> replacements = replacements();

        Arrays.stream(beanFactory.getBeanDefinitionNames())
                .map(beanFactory::getBeanDefinition)
                .map(BeanDefinition::getConstructorArgumentValues)
                .map(ConstructorArgumentValues::getIndexedArgumentValues)
                .map(Map::values)
                .flatMap(Collection::stream)
                .forEach(holder -> replaceRefs(holder, replacements));
    }

    private Map<String, BeanMetadataElement> replacements() {
        final Map<String, BeanMetadataElement> replacements = new HashMap<>();

        if (any(client -> client.getTracing().getEnabled())) {
            replacements.put(TRACER_REF, getBeanRef(Tracer.class, "tracer"));
        }

        if (any(client -> client.getLogging().getEnabled())) {
            replacements.put(LOGBOOK_REF, getBeanRef(Logbook.class, "logbook"));
        }

        if (any(client -> client.getMetrics().getEnabled())) {
            replacements.put(METER_REGISTRY_REF, getBeanRef(MeterRegistry.class, "meterRegistry"));
        }

        return replacements;
    }

    private void replaceRefs(final ValueHolder holder, final Map<String, BeanMetadataElement> replacements) {
        Optional.ofNullable(holder.getValue())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(replacements::get)
                .ifPresent(holder::setValue);
    }

    private boolean any(final Predicate<RiptideProperties.Client> predicate) {
        return properties.getClients().values().stream().anyMatch(predicate);
    }

    @VisibleForTesting
    BeanReference getBeanRef(final Class type, final String argName) {
        final Map<String, BeanDefinition> definitions = new HashMap<>();
        // search primary bean definition
        for (final String beanName : beanFactory.getBeanNamesForType(type)) {
            final BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition.isPrimary()) {
                return Registry.ref(beanName);
            }
            definitions.put(beanName, beanDefinition);
        }

        // resolve by name
        final BeanDefinition beanDefinition = definitions.get(argName);
        if (beanDefinition != null) {
            return Registry.ref(argName);
        }

        // if only one candidate present use it
        if (definitions.size() == 1) {
            return Registry.ref(definitions.keySet().iterator().next());
        }

        throw new NoSuchBeanDefinitionException(type);
    }

}
