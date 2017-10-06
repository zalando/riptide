package org.zalando.riptide.spring;

import com.codahale.metrics.MetricRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.zalando.riptide.Http;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@Component
public final class SpringContextTest {

    private static final String FACTORY_BEAN_NAME = "MyBean";

    @Configuration
    @Import({DefaultTestConfiguration.class, BeanImporter.class})
    public static class TestConfiguration {

    }

    public static class BeanImporter implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(final AnnotationMetadata annotationMetadata, final BeanDefinitionRegistry beanDefinitionRegistry) {
            final BeanDefinitionBuilder builder = genericBeanDefinition(BeanFactory.class);
            beanDefinitionRegistry.registerBeanDefinition(FACTORY_BEAN_NAME, builder.getBeanDefinition());
        }
    }

    public static class BeanFactory extends AbstractFactoryBean<Object> {

        private final MetricRegistry dependency;

        @Autowired
        public BeanFactory(final MetricRegistry dependency) {
            this.dependency = dependency;
        }

        @Override
        public Class<?> getObjectType() {
            return String.class;
        }

        @Override
        protected Object createInstance() throws Exception {
            return dependency;
        }
    }

    @Autowired
    @Qualifier("example")
    private Http example;

    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldProvideFrameworkBean() throws Exception {
        assertThat(example, is(notNullValue()));
    }

    @Test
    public void shouldProvideFactoryBean() throws Exception {
        assertThat(context.getBean(FACTORY_BEAN_NAME), is(notNullValue()));
    }

}
