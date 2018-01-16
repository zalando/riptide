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
    public void capture(final T result) {
        final boolean captured = reference.compareAndSet(null, Optional.ofNullable(result));

        if (!captured) {
            throw new IllegalStateException("Already captured");
        }
    }

    @Override
    public T retrieve() {
        @Nullable final Optional<T> value = reference.get();

        if (value == null) {
            throw new CaptureException();
        }

        return value.orElse(null);
    }

}
