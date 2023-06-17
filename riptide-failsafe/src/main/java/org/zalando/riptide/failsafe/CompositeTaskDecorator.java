package org.zalando.riptide.failsafe;

import dev.failsafe.function.ContextualSupplier;
import lombok.AllArgsConstructor;

import java.util.Collection;

@AllArgsConstructor
final class CompositeTaskDecorator implements TaskDecorator {

    private final Collection<TaskDecorator> decorators;

    @Override
    public <T, R> ContextualSupplier<T, R> decorate(final ContextualSupplier<T, R> supplier) {
        ContextualSupplier<T, R> result = supplier;
        for (final TaskDecorator decorator : decorators) {
            result = decorator.decorate(result);
        }
        return result;
    }

}
