package org.zalando.riptide;

import java.util.Collection;

final class CompoundPlugin implements Plugin {

    private final Collection<Plugin> plugins;

    CompoundPlugin(final Collection<Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public RequestExecution beforeSend(final RequestExecution execution) {
        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = plugin.beforeSend(result);
        }

        return result;
    }

    @Override
    public RequestExecution beforeDispatch(final RequestExecution execution) {
        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = plugin.beforeDispatch(result);
        }

        return result;
    }

}
