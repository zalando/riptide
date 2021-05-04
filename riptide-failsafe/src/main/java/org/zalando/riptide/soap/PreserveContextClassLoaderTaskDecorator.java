package org.zalando.riptide.soap;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import net.jodah.failsafe.function.ContextualSupplier;
import org.apiguardian.api.API;
import org.zalando.riptide.failsafe.TaskDecorator;

/**
 * @see <a href="https://github.com/zalando/riptide/issues/953">JAXB + ForkJoinPool</a>
 */
@API(status = EXPERIMENTAL)
public class PreserveContextClassLoaderTaskDecorator implements TaskDecorator {

  private final ClassLoader loader = Thread.currentThread().getContextClassLoader();

  @Override
  public <T> ContextualSupplier<T> decorate(ContextualSupplier<T> supplier) {
    return context -> {
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(loader);
        return supplier.get(context);
      } finally {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
      }
    };
  }
}
