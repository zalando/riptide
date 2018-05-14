package org.zalando.riptide.spring;

import static org.springframework.util.ClassUtils.getDefaultClassLoader;
import static org.springframework.util.ClassUtils.isPresent;

final class Dependencies {

    private Dependencies() {
        
    }

    static void ifPresent(final String name, final Runnable runnable) {
        if (isPresent(name, getDefaultClassLoader())) {
            runnable.run();
        }
    }

}
