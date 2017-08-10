package org.zalando.riptide;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
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
import static org.zalando.fauxpas.TryWith.tryWith;

/**
 *
 * @see RoutingTree
 * @see
 */
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

    static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return listOf(TypeToken.of(entityType));
    }

    @SuppressWarnings("serial")
    static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        final TypeToken<List<T>> listType = new TypeToken<List<T>>() {
            // nothing to implement!
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
            // nothing to implement!
        };

        return listType.where(elementType, entityType);
    }

    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final Class<T> entityType) {
        return responseEntityOf(TypeToken.of(entityType));
    }

    static <T> TypeToken<ResponseEntity<T>> responseEntityOf(final TypeToken<T> entityType) {
        final TypeToken<ResponseEntity<T>> responseEntityType = new TypeToken<ResponseEntity<T>>() {
            // nothing to implement!
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
            // nothing to implement!
        };

        return responseEntityType.where(elementType, entityType);
    }

    static ThrowingConsumer<ClientHttpResponse, RuntimeException> pass() {
        return response -> {
            // nothing to do!
        };
    }

    static ThrowingFunction<ClientHttpResponse, HttpHeaders, IOException> headers() {
        return HttpMessage::getHeaders;
    }

    static ThrowingFunction<ClientHttpResponse, URI, IOException> location() {
        return response ->
                response.getHeaders().getLocation();
    }

    static ThrowingConsumer<ClientHttpResponse, IOException> noRoute() {
        return response -> {
            throw new NoRouteException(response);
        };
    }

    static <X extends Exception> ThrowingConsumer<X, IOException> propagate() {
        return entity -> {
            propagateIfPossible(entity, IOException.class);
            throw new IOException(entity);
        };
    }

    static <T> Adapter<ClientHttpResponse, T> to(
            final ThrowingFunction<ClientHttpResponse, T, ? extends Exception> function) {
        return consumer ->
                response -> consumer.tryAccept(function.tryApply(response));
    }

    @FunctionalInterface
    interface Adapter<T, R> {
        ThrowingConsumer<T, Exception> andThen(final ThrowingConsumer<R, ? extends Exception> consumer);
    }

}
