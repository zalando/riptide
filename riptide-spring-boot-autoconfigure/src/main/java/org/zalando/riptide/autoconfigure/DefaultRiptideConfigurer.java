package org.zalando.riptide.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutorService;
import io.opentracing.contrib.concurrent.TracedScheduledExecutorService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.zalando.logbook.Logbook;
import org.zalando.riptide.failsafe.CircuitBreakerListener;
import org.zalando.riptide.failsafe.RetryListener;
import org.zalando.riptide.logbook.LogbookPlugin;
import org.zalando.riptide.metrics.MetricsPlugin;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.util.Optional;

import static org.zalando.riptide.autoconfigure.ValueConstants.LOGBOOK_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.METER_REGISTRY_REF;
import static org.zalando.riptide.autoconfigure.ValueConstants.TRACER_REF;

@AllArgsConstructor
class DefaultRiptideConfigurer {
    private final ConfigurableListableBeanFactory beanFactory;
    private final RiptideProperties properties;

    void register() {
        properties.getClients().forEach(this::configure);
    }

    private void configure(final String id, final RiptideProperties.Client client) {
        if (client.getTracing().getEnabled()) {
            final BeanDefinition tracerRef = getBeanRef(Tracer.class);

            findBeanDefinition(id, TracedExecutorService.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));

            findBeanDefinition(id, OpenTracingPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));

            findBeanDefinition(id, TracedScheduledExecutorService.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, TRACER_REF, tracerRef));
        }

        if (client.getLogging().getEnabled()) {
            final BeanDefinition logbookRef = getBeanRef(Logbook.class);

            findBeanDefinition(id, LogbookPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, LOGBOOK_REF, logbookRef));
        }

        if (client.getMetrics().getEnabled()) {
            final BeanDefinition meterRegistryRef = getBeanRef(MeterRegistry.class);

            findBeanDefinition(id, MetricsPlugin.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));

            findBeanDefinition(id, RetryListener.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));

            findBeanDefinition(id, CircuitBreakerListener.class)
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, METER_REGISTRY_REF, meterRegistryRef));
        }
    }

    private BeanDefinition getBeanRef(Class type) {
        final String[] names = beanFactory.getBeanNamesForType(type);
        BeanDefinition definition = null;
        for (final String name : names) {
            definition = beanFactory.getBeanDefinition(name);
            if (definition.isPrimary()) {
                return definition;
            }
        }
        return definition;
    }

    private Optional<BeanDefinition> findBeanDefinition(final String id, final Class<?> type) {
        return findBeanDefinition(Name.name(id, type));
    }

    private Optional<BeanDefinition> findBeanDefinition(final Name name) {
        try {
            return Optional.of(beanFactory.getBeanDefinition(name.toNormalizedString()));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    private void replaceConstructorArgumentWithBean(final BeanDefinition bd, final String arg, final BeanDefinition ref) {
        bd.getConstructorArgumentValues()
          .getIndexedArgumentValues()
          .values().stream()
          .filter(holder -> arg.equals(holder.getValue()))
          .forEach(holder -> holder.setValue(ref));
    }
}
