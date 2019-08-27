package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.http.converter.*;
import org.springframework.http.converter.json.*;
import org.springframework.stereotype.*;

import java.util.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*;

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
        assertThat(converters, hasItem(instanceOf(MappingJackson2HttpMessageConverter.class)));
        assertThat(converters, hasItem(hasToString(containsString("StreamConverter"))));
    }
}
