package org.zalando.riptide.autoconfigure;

import io.opentracing.*;
import io.opentracing.noop.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(PrimaryTracerConfiguration.class);
        context.refresh();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        BeanDefinition bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.isPrimary()).isTrue();
    }

    @Test
    void shouldBeanDefinitionIfSingleBeanRegisteredForType() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SingleTracerConfiguration.class);
        context.refresh();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        BeanDefinition bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.getFactoryMethodName()).isEqualTo("opentracingTracer");
    }

    @Test
    void shouldFindBeanDefinitionByNameIfNoPrimaryBeanAvailable() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DoubleTracerConfiguration.class);
        context.refresh();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        BeanDefinition bd = configurer.getBeanRef(Tracer.class, "tracer");
        assertThat(bd.getFactoryMethodName()).isEqualTo("tracer");
    }

    @Test
    void shouldFailIfMultipleBeanFoundWithoutCorrespondingName() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(DoubleTracerConfiguration.class);
        context.refresh();
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        DefaultRiptideConfigurer configurer = new DefaultRiptideConfigurer(beanFactory, null);
        assertThrows(NoSuchBeanDefinitionException.class,
                     () -> configurer.getBeanRef(Tracer.class, "opentracingTracer"));
    }
}
