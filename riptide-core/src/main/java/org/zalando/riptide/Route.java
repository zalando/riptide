package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.fauxpas.ThrowingRunnable;

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

}
