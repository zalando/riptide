package org.zalando.riptide.failsafe;

import net.jodah.failsafe.function.ContextualSupplier;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface TaskDecorator {

    <T> ContextualSupplier<T> decorate(ContextualSupplier<T> supplier);

    static TaskDecorator identity() {
        return new TaskDecorator() {
            @Override
            public <T> ContextualSupplier<T> decorate(final ContextualSupplier<T> supplier) {
                return supplier;
            }
        };
    }

}
