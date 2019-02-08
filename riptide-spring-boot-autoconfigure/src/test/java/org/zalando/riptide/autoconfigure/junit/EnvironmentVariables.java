package org.zalando.riptide.autoconfigure.junit;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Map;

import static java.lang.System.getenv;

public final class EnvironmentVariables {

    private final Map<String, String> original;

    public EnvironmentVariables() {
        this.original = ImmutableMap.copyOf(System.getenv());
    }

    public void set(String name, String value) {
        set(caseSensitiveEnvironment(), name, value);
        set(caseInsensitiveEnvironment(), name, value);
    }

    public void restore() {
        restore(caseSensitiveEnvironment());
        restore(caseInsensitiveEnvironment());
    }

    private void set(@Nullable final Map<String, String> variables, final String name, final String value) {
        if (variables != null) {
            if (value == null) {
                variables.remove(name);
            } else
                variables.put(name, value);
        }
    }

    private void restore(@Nullable Map<String, String> variables) {
        if (variables == null) {
            return;
        }

        variables.clear();
        variables.putAll(original);
    }

    @SneakyThrows
    private static Map<String, String> caseSensitiveEnvironment() {
        final Map<String, String> env = getenv();
        Class<?> classOfMap = env.getClass();
        return getFieldValue(classOfMap, env, "m");
    }

    @Nullable
    @SneakyThrows
    private static Map<String, String> caseInsensitiveEnvironment() {
        try {
            final Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            return getFieldValue(processEnvironment, null, "theCaseInsensitiveEnvironment");
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows(IllegalAccessException.class)
    private static Map<String, String> getFieldValue(Class<?> klass, final Object object, final String name)
            throws NoSuchFieldException {
        final Field field = klass.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, String>) field.get(object);
    }

}
