package org.zalando.riptide.autoconfigure;

import org.junit.jupiter.api.*;

import static org.mockito.Mockito.*;
import static org.zalando.riptide.autoconfigure.Dependencies.*;

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
