package org.zalando.riptide.capture;

import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.fauxpas.*;

import javax.annotation.*;
import java.util.function.*;

import static org.apiguardian.api.API.Status.*;

@API(status = STABLE)
public interface Capture<T> extends ThrowingConsumer<T, RuntimeException>, Function<ClientHttpResponse, T> {

    void capture(@Nullable T result) throws IllegalStateException;

    T retrieve() throws CaptureException;

    @Override
    default void tryAccept(@Nullable final T result) {
        capture(result);
    }

    @Override
    default T apply(final ClientHttpResponse response) {
        return retrieve();
    }

    static <T> Capture<T> empty() {
        return new DefaultCapture<>();
    }

}
