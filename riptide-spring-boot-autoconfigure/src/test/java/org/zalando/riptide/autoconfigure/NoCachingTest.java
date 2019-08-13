package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class NoCachingTest {
    @Test
    void shouldReadClassDefinitionWithoutCache() {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(HttpClientFactory.class);
        assertNotNull(methods);
    }
}
