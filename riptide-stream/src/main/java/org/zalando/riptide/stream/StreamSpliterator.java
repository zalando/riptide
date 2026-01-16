package org.zalando.riptide.stream;


import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JavaType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.function.Consumer;

final class StreamSpliterator<T> implements Spliterator<T> {

    private final JavaType type;
    private final JsonParser parser;
    private final boolean isNotStreamOfArrays;

    StreamSpliterator(final JavaType type, final JsonParser parser) {
        this.type = type;
        this.parser = parser;
        this.isNotStreamOfArrays = !type.isArrayType() && !type.isCollectionLikeType();
    }

    @Override
    public boolean tryAdvance(final Consumer<? super T> action) {
        try {
            final JsonToken token = parser.nextToken();

            if (token == null) {
                return false;
            }

            if (isNotStreamOfArrays && skipArrayTokens(token)) {
                return false;
            }

            final T value = parser.readValueAs(type);
            action.accept(value);
            return true;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean skipArrayTokens(final JsonToken token) throws IOException {
        return switch (token) {
            case START_ARRAY -> {
                parser.nextToken();
                yield false;
            }
            case END_ARRAY -> {
                parser.nextToken();
                yield true;
            }
            default -> false;
        };
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
