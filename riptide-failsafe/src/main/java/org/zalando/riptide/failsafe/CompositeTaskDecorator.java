package org.zalando.riptide.failsafe;

import java.util.List;
import lombok.AllArgsConstructor;
import net.jodah.failsafe.function.ContextualSupplier;

@AllArgsConstructor
public class CompositeTaskDecorator implements TaskDecorator {

  private final List<TaskDecorator> decorators;

  @Override
  public <T> ContextualSupplier<T> decorate(ContextualSupplier<T> supplier) {
    ContextualSupplier<T> result = supplier;
    for (TaskDecorator decorator: decorators) {
      result = decorator.decorate(result);
    }
    return result;
  }
}
