package org.zalando.riptide.autoconfigure;

import java.util.Optional;
import java.util.function.Supplier;

import static org.springframework.util.ClassUtils.getDefaultClassLoader;
import static org.springframework.util.ClassUtils.isPresent;

final class Dependencies {

    private Dependencies() {
        
    }

    static void ifPresent(final String name, final Runnable runnable) {
        ifPresent(name, () -> {
            runnable.run();
            return null;
        });
    }

    static <T> Optional<T> ifPresent(final String name, final Supplier<T> supplier) {
        if (isPresent(name, getDefaultClassLoader())) {
            return Optional.ofNullable(supplier.get());
        } else {
            return Optional.empty();
        }
    }

}
