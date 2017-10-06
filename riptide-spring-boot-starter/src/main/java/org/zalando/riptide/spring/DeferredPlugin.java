package org.zalando.riptide.spring;

import com.google.common.annotations.VisibleForTesting;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

final class DeferredPlugin implements Plugin {

    // turns out this is actually easier to use than AtomicReference since we want to use a lazy set-once
    private final ConcurrentMap<Plugin, Plugin> delegate = new ConcurrentHashMap<>();

    private final Class<? extends Plugin> type;
    private final Supplier<? extends Plugin> supplier;

    DeferredPlugin(final Class<? extends Plugin> type, final Supplier<? extends Plugin> supplier) {
        this.type = type;
        this.supplier = supplier;
    }

    @Override
    public RequestExecution prepare(@Nonnull final RequestArguments arguments,
            @Nonnull final RequestExecution execution) {
        return getDelegate().prepare(arguments, execution);
    }

    private Plugin getDelegate() {
        return delegate.computeIfAbsent(this, $ -> supplier.get());
    }

    @VisibleForTesting
    Class<? extends Plugin> getType() {
        return type;
    }

}
