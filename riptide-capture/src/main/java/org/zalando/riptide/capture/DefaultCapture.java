package org.zalando.riptide.capture;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
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
        reference.compareAndSet(null, Optional.ofNullable(input));
    }

    @Override
    public T apply(final Void result) {
        final Optional<T> value = reference.get();
        checkPresent(value);
        return value.orElse(null);
    }

    private void checkPresent(@Nullable final Optional<T> value) {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
    }

}
