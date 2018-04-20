package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.fauxpas.ThrowingFunction;
import org.zalando.fauxpas.ThrowingRunnable;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Throwables.propagateIfPossible;
import static org.apiguardian.api.API.Status.DEPRECATED;
import static org.apiguardian.api.API.Status.STABLE;
import static org.zalando.fauxpas.TryWith.tryWith;

/**
 *
 * @see RoutingTree
 */
@API(status = STABLE)
@FunctionalInterface
public interface Route {

    void execute(final ClientHttpResponse response, final MessageReader reader) throws Exception;

    static Route call(final ThrowingRunnable<? extends Exception> runnable) {
        return (response, reader) ->
                tryWith(response, (ClientHttpResponse ignored) -> runnable.tryRun());
    }

    static Route call(final ThrowingConsumer<ClientHttpResponse, ? extends Exception> consumer) {
        return (response, reader) ->
                tryWith(response, consumer);
    }

    static <I> Route call(final Class<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final ThrowingConsumer<I, ? extends Exception> consumer) {
        return (response, reader) -> {
            final I body = reader.read(type, response);
            consumer.tryAccept(body);
        };
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return Types.listOf(entityType);
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        return Types.listOf(entityType);
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final Class<T> entityType) {
        return Types.responseEntityOf(TypeToken.of(entityType));
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final TypeToken<T> entityType) {
        return Types.responseEntityOf(entityType);
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static ThrowingConsumer<ClientHttpResponse, RuntimeException> pass() {
        return response -> {
            // nothing to do!
        };
    }

    @API(status = DEPRECATED, since = "2.5.1")
    @Deprecated//(since = "2.5.1", forRemoval = true)
    static ThrowingFunction<ClientHttpResponse, HttpHeaders, IOException> headers() {
        return HttpMessage::getHeaders;
    }

    @API(status = DEPRECATED, since = "2.5.1")
    @Deprecated//(since = "2.5.1", forRemoval = true)
    static ThrowingFunction<ClientHttpResponse, URI, IOException> location() {
        return response ->
                response.getHeaders().getLocation();
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static ThrowingConsumer<ClientHttpResponse, IOException> noRoute() {
        return response -> {
            throw new NoRouteException(response);
        };
    }

    @API(status = DEPRECATED, since = "2.6.0")
    @Deprecated//(since = "2.6.0", forRemoval = true)
    static <X extends Exception> ThrowingConsumer<X, IOException> propagate() {
        return entity -> {
            propagateIfPossible(entity, IOException.class);
            throw new IOException(entity);
        };
    }

    @API(status = DEPRECATED, since = "2.5.1")
    @Deprecated//(since = "2.5.1", forRemoval = true)
    static <T> Adapter<ClientHttpResponse, T> to(
            final ThrowingFunction<ClientHttpResponse, T, ? extends Exception> function) {
        return consumer ->
                response -> consumer.tryAccept(function.tryApply(response));
    }

    @API(status = DEPRECATED, since = "2.5.1")
    @Deprecated//(since = "2.5.1", forRemoval = true)
    @FunctionalInterface
    interface Adapter<T, R> {
        ThrowingConsumer<T, Exception> andThen(final ThrowingConsumer<R, ? extends Exception> consumer);
    }

}
