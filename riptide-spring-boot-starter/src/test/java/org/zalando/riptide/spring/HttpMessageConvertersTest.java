package org.zalando.riptide.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = DefaultTestConfiguration.class)
@Component
public final class HttpMessageConvertersTest {

    @Autowired
    @Qualifier("example")
    private ClientHttpMessageConverters unit;

    @Test
    public void shouldRegisterOnlyRegisteredConverters() {
        final List<HttpMessageConverter<?>> converters = unit.getConverters();
        assertThat(converters, hasSize(3));
        assertThat(converters, hasItem(instanceOf(StringHttpMessageConverter.class)));
        assertThat(converters, hasItem(instanceOf(MappingJackson2HttpMessageConverter.class)));
        assertThat(converters, hasItem(hasToString(containsString("StreamConverter"))));
    }
}
