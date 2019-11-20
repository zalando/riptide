package org.zalando.riptide.autoconfigure;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DefaultRiptideConfigurerTest {
    @TestConfiguration
    static class PrimaryTracerConfiguration {
        @Bean
        @Primary
        Tracer primaryTracer() {
            return NoopTracerFactory.create();
        }

        @Bean
        Tracer secondaryTracer() {
            return NoopTracerFactory.create();
        }
    }

    @TestConfiguration
    static class SingleTracerConfiguration {
        @Bean
        Tracer opentracingTracer() {
            return NoopTracerFactory.create();
        }
    }

    @TestConfiguration
    static class DoubleTracerConfiguration {
        @Bean
        Tracer tracer() {
            return NoopTracerFactory.create();
        }

        @Bean
        Tracer secondaryTracer() {
            return NoopTracerFactory.create();
        }
    }

    @Test
    void shouldFindPrimaryBeanDefinitionIfAvailable() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(PrimaryTracerConfiguration.class);
        context.refresh();
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        final DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        final BeanReference bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.getBeanName()).isEqualTo("primaryTracer");
    }

    @Test
    void shouldBeanDefinitionIfSingleBeanRegisteredForType() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SingleTracerConfiguration.class);
        context.refresh();
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        final DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        final BeanReference bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.getBeanName()).isEqualTo("opentracingTracer");
    }

    @Test
    void shouldFindBeanDefinitionByNameIfNoPrimaryBeanAvailable() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DoubleTracerConfiguration.class);
        context.refresh();
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        final DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        final BeanReference bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.getBeanName()).isEqualTo("tracer");
    }

    @Test
    void shouldFailIfMultipleBeanFoundWithoutCorrespondingName() {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DoubleTracerConfiguration.class);
        context.refresh();
        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        final DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        assertThrows(NoSuchBeanDefinitionException.class,
                     () -> configurer.getBeanRef(Tracer.class, "opentracingTracer"));
    }
}
