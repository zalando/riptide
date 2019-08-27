package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.*;
import io.micrometer.core.instrument.*;
import io.opentracing.*;
import io.opentracing.contrib.concurrent.*;
import lombok.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.zalando.logbook.*;
import org.zalando.riptide.failsafe.*;
import org.zalando.riptide.logbook.*;
import org.zalando.riptide.micrometer.*;
import org.zalando.riptide.opentracing.*;

import java.util.*;

import static org.zalando.riptide.autoconfigure.ValueConstants.*;

@AllArgsConstructor
class DefaultRiptideConfigurer {
    private final ConfigurableListableBeanFactory beanFactory;
    private final RiptideProperties properties;

    void register() {
        properties.getClients().forEach(this::configure);
    }

    private void configure(final String id, final RiptideProperties.Client client) {
        if (client.getTracing().getEnabled()) {
            final BeanDefinition tracerRef = getBeanRef(Tracer.class, "tracer");

            findBeanDefinition(id, TracedExecutorService.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));

            findBeanDefinition(id, OpenTracingPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));

            findBeanDefinition(id, TracedScheduledExecutorService.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));
        }

        if (client.getLogging().getEnabled()) {
            final BeanDefinition logbookRef = getBeanRef(Logbook.class, "logbook");

            findBeanDefinition(id, LogbookPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, LOGBOOK_REF, logbookRef));
        }

        if (client.getMetrics().getEnabled()) {
            final BeanDefinition meterRegistryRef = getBeanRef(MeterRegistry.class, "meterRegistry");

            findBeanDefinition(id, MicrometerPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));

            findBeanDefinition(id, RetryListener.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));

            findBeanDefinition(id, CircuitBreakerListener.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));
        }
    }

    @VisibleForTesting
    BeanDefinition getBeanRef(Class type, String argName) {
        Map<String, BeanDefinition> definitions = new HashMap<>();
        // search primary bean definition
        for (String beanName : beanFactory.getBeanNamesForType(type)) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            if (beanDefinition.isPrimary()) {
                return beanDefinition;
            }
            definitions.put(beanName, beanDefinition);
        }

        // resolve by name
        BeanDefinition beanDefinition = definitions.get(argName);
        if (beanDefinition != null) {
            return beanDefinition;
        }

        // if only one candidate present use it
        if (definitions.size() == 1) {
            return definitions.values().iterator().next();
        }

        throw new NoSuchBeanDefinitionException(type);
    }

    private Optional<BeanDefinition> findBeanDefinition(final String id, final Class<?> type) {
        try {
            final Name name = Name.name(id, type);
            return Optional.of(beanFactory.getBeanDefinition(name.toNormalizedString()));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    private void replaceConstructorArgumentWithBean(final BeanDefinition bd, final String arg,
                                                    final BeanDefinition ref) {
        bd.getConstructorArgumentValues()
          .getIndexedArgumentValues()
          .values().stream()
          .filter(holder -> arg.equals(holder.getValue()))
          .forEach(holder -> holder.setValue(ref));
    }
}
