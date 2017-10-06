package org.zalando.riptide.spring;

import org.zalando.riptide.Plugin;

import java.util.List;
import java.util.function.Supplier;

final class Plugins {

    private final Supplier<List<Plugin>> plugins;

    Plugins(final Supplier<List<Plugin>> plugins) {
        this.plugins = plugins;
    }

    List<Plugin> resolvePlugins() {
        return plugins.get();
    }

}
