package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.function.Consumer;

final class ResponseSpliterator<T> implements Spliterator<T> {
    private final ClientHttpResponse clientHttpResponse;
    private final MessageReader messageReader;
    private final TypeToken<T> typeToken;

    ResponseSpliterator(ClientHttpResponse clientHttpResponse, MessageReader messageReader, TypeToken<T> typeToken) {
        this.clientHttpResponse = clientHttpResponse;
        this.messageReader = messageReader;
        this.typeToken = typeToken;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        try {
            final T entity = messageReader.readEntity(typeToken, clientHttpResponse);
            action.accept(entity);
            return true;
        } catch (IOException e) {
            // TODO: just return false?
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED | IMMUTABLE;
    }
}
