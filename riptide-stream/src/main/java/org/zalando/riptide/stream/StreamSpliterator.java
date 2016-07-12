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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

class StreamSpliterator<T> implements Spliterator<T> {

    private final JsonParser parser;
    private final ObjectMapper mapper;
    private final JavaType type;

    StreamSpliterator(JavaType type, JsonParser parser, ObjectMapper mapper) {
        this.type = type;
        this.parser = parser;
        this.mapper = mapper;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        try {
            JsonToken token = parser.nextToken();
            if (token == null) {
                return false;
            }
            if (!type.isArrayType() && !type.isCollectionLikeType()) {
                switch (token) {
                case START_ARRAY:
                    parser.nextToken();
                    break;

                case END_ARRAY:
                    parser.nextToken();
                    return false;

                default:
                    // nothing to do.
                }
            }

            final T value = mapper.readValue(parser, type);
            action.accept(value);
            return true;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
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