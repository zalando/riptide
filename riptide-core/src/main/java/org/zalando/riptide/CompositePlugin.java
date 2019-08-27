package org.zalando.riptide;

import java.util.*;
import java.util.function.*;

final class CompositePlugin implements Plugin {

    private final Collection<Plugin> plugins;

    CompositePlugin(final Collection<Plugin> plugins) {
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
    public RequestExecution aroundSerialization(final RequestExecution execution) {
        return combine(execution, Plugin::aroundSerialization);
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return combine(execution, Plugin::aroundNetwork);
    }

    private RequestExecution combine(final RequestExecution execution,
            final BiFunction<Plugin, RequestExecution, RequestExecution> combiner) {

        RequestExecution result = execution;

        for (final Plugin plugin : plugins) {
            result = apply(plugin, result, combiner);
        }

        return result;
    }

    private RequestExecution apply(final Plugin plugin, final RequestExecution before,
            final BiFunction<Plugin, RequestExecution, RequestExecution> combiner) {

        final RequestExecution after = combiner.apply(plugin, before);

        if (Objects.equals(before, after)) {
            return after;
        }

        return new GuardedRequestExecution(after);
    }

}
