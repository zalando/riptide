package org.zalando.riptide.capture;

import org.apiguardian.api.API;
import org.zalando.fauxpas.ThrowingConsumer;

import java.util.function.Function;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface Capture<T> extends ThrowingConsumer<T, RuntimeException>, Function<Void, T> {

    static <T> Capture<T> empty() {
        return new DefaultCapture<>();
    }

}
