package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(classes = DefaultTestConfiguration.class, webEnvironment = NONE)
@Component
final class HttpMessageConvertersTest {

    @Autowired
    @Qualifier("example")
    private ClientHttpMessageConverters unit;

    @Test
    void shouldRegisterOnlyRegisteredConverters() {
        final List<HttpMessageConverter<?>> converters = unit.getConverters();
        assertThat(converters, hasSize(3));
        assertThat(converters, hasItem(instanceOf(StringHttpMessageConverter.class)));
        assertThat(converters, hasItem(instanceOf(JacksonJsonHttpMessageConverter.class)));
        assertThat(converters, hasItem(hasToString(containsString("StreamConverter"))));
    }
}
