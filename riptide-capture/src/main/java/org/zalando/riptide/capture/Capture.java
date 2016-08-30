package org.zalando.riptide.capture;

import org.zalando.fauxpas.ThrowingConsumer;

import java.util.function.Function;

public interface Capture<T> extends ThrowingConsumer<T, RuntimeException>, Function<Void, T> {

    static <T> Capture<T> empty() {
        return new DefaultCapture<>();
    }

}
