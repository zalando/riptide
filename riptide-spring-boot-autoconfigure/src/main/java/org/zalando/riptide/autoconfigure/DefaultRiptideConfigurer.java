package org.zalando.riptide.autoconfigure;

import com.google.common.annotations.VisibleForTesting;
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
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.opentracing.OpenTracingPlugin;

import java.util.HashMap;
import java.util.Map;
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
        return findBeanDefinition(Name.name(id, type));
    }

    private Optional<BeanDefinition> findBeanDefinition(final Name name) {
        try {
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
