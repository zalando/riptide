package org.zalando.riptide;

import java.util.Collection;

final class CompoundPlugin implements Plugin {

    private final Collection<Plugin> plugins;

    CompoundPlugin(final Collection<Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public RequestExecution interceptBeforeRouting(final RequestArguments arguments, final RequestExecution execution) {
        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = plugin.interceptBeforeRouting(arguments, result);
        }

        return result;
    }

    @Override
    public RequestExecution prepare(final RequestArguments arguments, final RequestExecution execution) {
        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = plugin.prepare(arguments, result);
        }

        return result;
    }

}
