package org.zalando.riptide.stream;

/*
 * ⁣​
 * Riptide: Stream
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JavaType;

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

            final T value = parser.getCodec().readValue(parser, type);
            action.accept(value);
            return true;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean skipArrayTokens(final JsonToken token) throws IOException {
        switch (token) {
            case START_ARRAY:
                parser.nextToken();
                return false;

            case END_ARRAY:
                parser.nextToken();
                return true;

            default:
                return false;
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