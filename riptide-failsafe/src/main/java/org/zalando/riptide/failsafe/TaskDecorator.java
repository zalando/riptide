package org.zalando.riptide.failsafe;

import net.jodah.failsafe.function.ContextualSupplier;
import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.Collection;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@FunctionalInterface
public interface TaskDecorator {

    <T> ContextualSupplier<T> decorate(ContextualSupplier<T> supplier);

    static TaskDecorator identity() {
        return composite();
    }

    static TaskDecorator composite(final TaskDecorator... decorators) {
        return composite(Arrays.asList(decorators));
    }

    static TaskDecorator composite(final Collection<TaskDecorator> decorators) {
        return new CompositeTaskDecorator(decorators);
    }

}
