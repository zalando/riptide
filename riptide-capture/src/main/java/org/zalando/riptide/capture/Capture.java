package org.zalando.riptide.capture;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingConsumer;

import java.util.function.Function;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface Capture<T> extends ThrowingConsumer<T, RuntimeException>, Function<ClientHttpResponse, T> {

    void capture(T result) throws IllegalStateException;

    T retrieve() throws CaptureException;

    @Override
    default void tryAccept(final T result) {
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
