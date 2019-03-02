package org.zalando.riptide;

import java.util.Collection;
import java.util.function.BiFunction;

final class CompoundPlugin implements Plugin {

    private final Collection<Plugin> plugins;

    CompoundPlugin(final Collection<Plugin> plugins) {
        this.plugins = plugins;
    }

    @Override
    public RequestExecution aroundAsync(final RequestExecution execution) {
        return combine(execution, Plugin::aroundAsync);
    }

    @Override
    public RequestExecution aroundDispatch(final RequestExecution execution) {
        return combine(execution, Plugin::aroundDispatch);
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return combine(execution, Plugin::aroundNetwork);
    }

    private RequestExecution combine(final RequestExecution execution,
            final BiFunction<Plugin, RequestExecution, RequestExecution> combiner) {

        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = combiner.apply(plugin, result);
        }

        return result;
    }

}
