package org.zalando.riptide.failsafe;

import lombok.AllArgsConstructor;
import net.jodah.failsafe.function.ContextualSupplier;

import java.util.Collection;

@AllArgsConstructor
final class CompositeTaskDecorator implements TaskDecorator {

    private final Collection<TaskDecorator> decorators;

    @Override
    public <T> ContextualSupplier<T> decorate(final ContextualSupplier<T> supplier) {
        ContextualSupplier<T> result = supplier;
        for (final TaskDecorator decorator : decorators) {
            result = decorator.decorate(result);
        }
        return result;
    }

}
