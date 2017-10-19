package org.zalando.riptide.spring;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.spring.Dependencies.ifPresent;

public final class DependenciesTest {

    private final Runnable runnable = mock(Runnable.class);

    @Test
    public void shouldRun() {
        ifPresent("java.lang.String", runnable);

        verify(runnable).run();
    }

    @Test
    public void shouldNotRun() {
        ifPresent("foo", runnable);

        verify(runnable, never()).run();
    }

}
