package org.zalando.riptide.capture;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultCapture<T> implements Capture<T> {

    /**
     * A reference holding the captured value. It can represent three different states:
     * <ol>
     *     <li>{@code null}: not captured</li>
     *     <li>{@code Optional.empty()}: captured null</li>
     *     <li>{@code Optional.of(..)}: captured non-null</li>
     * </ol>
     */
    private final AtomicReference<Optional<T>> reference = new AtomicReference<>();

    @Override
    public void tryAccept(@Nullable final T input) {
        final boolean captured = reference.compareAndSet(null, Optional.ofNullable(input));

        if (!captured) {
            throw new IllegalStateException("Already captured");
        }
    }

    @Override
    public T apply(final Void result) {
        @Nullable final Optional<T> value = reference.get();

        if (value == null) {
            throw new CaptureException();
        }

        return value.orElse(null);
    }

}
