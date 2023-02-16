package org.zalando.riptide.soap;

import dev.failsafe.function.ContextualSupplier;
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
        final ClassLoader invokingThreadCL = Thread.currentThread().getContextClassLoader();
        return context -> {
            final ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(invokingThreadCL);
                return supplier.get(context);
            } finally {
                Thread.currentThread().setContextClassLoader(originalCL);
            }
        };
    }

}
