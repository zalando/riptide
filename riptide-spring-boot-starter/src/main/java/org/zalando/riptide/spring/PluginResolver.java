package org.zalando.riptide.spring;

import org.zalando.riptide.Plugin;

@FunctionalInterface
public interface PluginResolver {

    Plugin resolve(final String name);

}
