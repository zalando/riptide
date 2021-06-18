package org.zalando.riptide.soap;

import net.jodah.failsafe.function.ContextualSupplier;
import org.apiguardian.api.API;
import org.zalando.riptide.failsafe.TaskDecorator;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * @see <a href="https://github.com/zalando/riptide/issues/953">JAXB + ForkJoinPool</a>
 */
@API(status = EXPERIMENTAL)
public final class PreserveContextClassLoaderTaskDecorator implements TaskDecorator {

    @Override
    public <T> ContextualSupplier<T> decorate(final ContextualSupplier<T> supplier) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader original = currentThread.getContextClassLoader();
        return context -> {
            try {
                currentThread.setContextClassLoader(original);
                return supplier.get(context);
            } finally {
                currentThread.setContextClassLoader(original);
            }
        };
    }

}
