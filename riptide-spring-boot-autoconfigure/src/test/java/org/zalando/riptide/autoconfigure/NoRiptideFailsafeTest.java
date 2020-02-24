package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.util.ReflectionUtils.getUniqueDeclaredMethods;

final class NoRiptideFailsafeTest {
    @Test
    void shouldReadMicrometerPluginDefinitionWithoutFailsafePlugin() {
        assertNotNull(getUniqueDeclaredMethods(MicrometerPluginFactory.class));
    }

    @Test
    void shouldReadDefaultRiptideRegistrarDefinitionWithoutFailsafePlugin() {
        assertNotNull(getUniqueDeclaredMethods(DefaultRiptideRegistrar.class));
    }
}
