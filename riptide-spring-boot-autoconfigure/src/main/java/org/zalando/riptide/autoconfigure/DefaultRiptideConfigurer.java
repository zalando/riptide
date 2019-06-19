package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.zalando.logbook.Logbook;

import java.util.Optional;
import java.util.stream.Stream;

@AllArgsConstructor
class DefaultRiptideConfigurer {
    private final ConfigurableListableBeanFactory beanFactory;
    private final RiptideProperties properties;

    void register() {
        properties.getClients().forEach(this::configure);
    }

    private void configure(String id, RiptideProperties.Client client) {
        if (client.getTracing().getEnabled()) {
            final BeanDefinition tracerRef = getTracerBeanRef(Tracer.class);

            findBeanDefinition(id + "TracedExecutorService")
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, "tracer", tracerRef));

            findBeanDefinition(id + "OpenTracingPlugin")
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, "tracer", tracerRef));

            findBeanDefinition(id + "TracedScheduledExecutorService")
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, "tracer", tracerRef));
        }

        if (client.getLogging().getEnabled()) {
            final BeanDefinition logbookRef = getTracerBeanRef(Logbook.class);

            findBeanDefinition(id + "LogbookPlugin")
                    .ifPresent(bd -> replaceConstructorArgumentWithBean(bd, "logbook", logbookRef));
        }

    }

    private BeanDefinition getTracerBeanRef(Class clazz) {
        return Stream.of(beanFactory.getBeanNamesForType(clazz))
                     .findFirst()
                     .map(beanFactory::getBeanDefinition)
                     .orElse(null);
    }

    private Optional<BeanDefinition> findBeanDefinition(String id) {
        try {
            return Optional.of(beanFactory.getBeanDefinition(id));
        } catch (NoSuchBeanDefinitionException e) {
            return Optional.empty();
        }
    }

    private void replaceConstructorArgumentWithBean(BeanDefinition bd, String arg, BeanDefinition ref) {
        bd.getConstructorArgumentValues()
          .getIndexedArgumentValues()
          .values().stream()
          .filter(holder -> arg.equals(holder.getValue()))
          .forEach(holder -> holder.setValue(ref));
    }
}
