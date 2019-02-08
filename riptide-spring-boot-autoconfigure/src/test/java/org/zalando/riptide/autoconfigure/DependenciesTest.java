package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.zalando.riptide.autoconfigure.Dependencies.ifPresent;

final class DependenciesTest {

    private final Runnable runnable = mock(Runnable.class);

    @Test
    void shouldRun() {
        ifPresent("java.lang.String", runnable);

        verify(runnable).run();
    }

    @Test
    void shouldNotRun() {
        ifPresent("foo", runnable);

        verify(runnable, never()).run();
    }

}
