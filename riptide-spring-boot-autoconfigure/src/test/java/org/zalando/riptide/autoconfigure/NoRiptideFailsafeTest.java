package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class NoRiptideFailsafeTest {
    @Test
    void shouldReadClassDefinitionWithoutFailsafePlugin() {
        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(MicrometerPluginFactory.class);
        assertNotNull(methods);
    }
}
