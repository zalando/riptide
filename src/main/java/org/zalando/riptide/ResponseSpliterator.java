package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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

import com.google.common.reflect.TypeToken;
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
