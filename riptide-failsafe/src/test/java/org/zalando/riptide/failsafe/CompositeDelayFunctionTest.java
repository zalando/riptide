package org.zalando.riptide.failsafe;

import dev.failsafe.ExecutionContext;
import dev.failsafe.function.ContextualSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zalando.riptide.failsafe.CompositeDelayFunction.composite;

@ExtendWith(MockitoExtension.class)
final class CompositeDelayFunctionTest {

    private final ContextualSupplier<String, Duration> first;
    private final ContextualSupplier<String, Duration> second;
    private final ContextualSupplier<String, Duration> unit;
    private final ExecutionContext<String> firstContext;
    private final ExecutionContext<String> secondContext;

    CompositeDelayFunctionTest(
            @Mock final ContextualSupplier<String, Duration> first,
            @Mock final ContextualSupplier<String, Duration> second,
            @Mock final ExecutionContext<String> firstContext,
            @Mock final ExecutionContext<String> secondContext) {
        this.first = first;
        this.second = second;
        this.firstContext = firstContext;
        this.secondContext = secondContext;
        this.unit = composite(first, second);
    }

    @BeforeEach
    void defaultBehavior() throws Throwable {
        // starting with Mockito 3.4.4, mocks will return Duration.ZERO instead of null, by default
        when(first.get(any())).thenReturn(null);
        when(first.get(any())).thenReturn(null);
    }

    @Test
    void shouldUseFirstNonNullDelay() throws Throwable {

        when(first.get(eq(firstContext))).thenReturn(Duration.ofSeconds(1));
        when(second.get(eq(secondContext))).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(1), unit.get(firstContext));
    }

    @Test
    void shouldReThrowException() throws Throwable {

        when(first.get(eq(firstContext))).thenThrow(new IllegalArgumentException("Wrong argument"));
        when(second.get(eq(secondContext))).thenReturn(Duration.ofSeconds(2));

        assertThrowsExactly(RuntimeException.class, () -> unit.get(firstContext));
    }

    @Test
    void shouldIgnoreNullDelay() throws Throwable {
        when(second.get(eq(secondContext))).thenReturn(Duration.ofSeconds(2));

        assertEquals(Duration.ofSeconds(2), unit.get(secondContext));
    }

    @Test
    void shouldFallbackToNullDelay() throws Throwable {
        when(first.get(eq(firstContext))).thenReturn(Duration.ofSeconds(1));
        when(second.get(eq(secondContext))).thenReturn(Duration.ofSeconds(2));

        assertNull(unit.get(mock(ExecutionContext.class)));
    }

}
