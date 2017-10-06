package org.zalando.riptide.spring;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.Invokable;
import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ClassUtils;
import org.zalando.riptide.OriginalStackTracePlugin;
import org.zalando.riptide.Plugin;

import java.lang.reflect.Constructor;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.springframework.util.ClassUtils.getDefaultClassLoader;
import static org.zalando.fauxpas.FauxPas.throwingFunction;
import static org.zalando.fauxpas.FauxPas.throwingSupplier;

public final class DefaultPluginResolver implements PluginResolver {

    private final ImmutableMap<String, Optional<Plugin>> plugins;

    public DefaultPluginResolver(final ListableBeanFactory factory) {
        this.plugins = ImmutableMap.of(
                "original-stack-trace", Optional.of(new OriginalStackTracePlugin()),
                "temporary-exception", defer(factory, "org.zalando.riptide.exceptions.TemporaryExceptionPlugin"),
                "hystrix", defer(factory, "org.zalando.riptide.hystrix.HystrixPlugin")
        );
    }

    @Hack
    @OhNoYouDidnt
    private static Optional<Plugin> defer(final ListableBeanFactory factory, final String typeName) {
        return Optional.of(typeName)
                .filter(DefaultPluginResolver::exists)
                .map(throwingFunction(DefaultPluginResolver::forName))
                .map(type -> defer(factory, type));
    }

    private static boolean exists(final String name) {
        return ClassUtils.isPresent(name, getDefaultClassLoader());
    }

    private static Class<? extends Plugin> forName(final String name) throws ClassNotFoundException {
        return ClassUtils.forName(name, getDefaultClassLoader()).asSubclass(Plugin.class);
    }

    private static DeferredPlugin defer(final ListableBeanFactory factory, final Class<? extends Plugin> type) {
        return new DeferredPlugin(type, throwingSupplier(() -> {
            if (factory.getBeanNamesForType(type).length > 0) {
                return factory.getBean(type);
            } else {
                final Constructor<? extends Plugin> constructor = type.getConstructor();
                return Invokable.from(constructor).invoke(null);
            }
        }));
    }

    @Override
    public Plugin resolve(final String name) {
        return plugins.getOrDefault(name, empty()).orElseThrow(() ->
                new IllegalArgumentException("Unknown plugin name: " + name));
    }

}
