package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;
import org.springframework.util.*;

import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

final class NoCachingTest {
    @Test
    void shouldReadClassDefinitionWithoutCache() {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(HttpClientFactory.class);
        assertNotNull(methods);
    }
}
