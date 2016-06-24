package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

@Immutable
public final class Capture {

    private final Stream<?> stream;

    Capture(final Stream<?> stream) {
        this.stream = checkNotNull(stream);
    }

    public <T> Optional<T> as(final Class<T> type) {
        return as(TypeToken.of(type));
    }

    public <T> Optional<T> as(final TypeToken<T> type) {
        try (final Stream<T> stream = stream(type)) {
            return stream.findFirst();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Stream<T> stream(final TypeToken<T> type) {
        return stream.map(v -> (T)v);
    }

    public <T> Stream<T> stream(final Class<T> type) {
        return stream.map(type::cast);
    }

    public <T> T to(final Class<T> type) {
        return to(TypeToken.of(type));
    }

    public <T> T to(final TypeToken<T> type) {
        return as(type).get();
    }

    static Capture none() {
        return new Capture(Stream.empty());
    }

    static <T> Capture valueOf(@Nullable final T value) {
        return new Capture(value == null ? Stream.empty() : Stream.of(value));
    }

    static <T> Capture ofStream(final Stream<T> stream) {
        return new Capture(stream);
    }

    static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return listOf(TypeToken.of(entityType));
    }

    static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        final TypeToken<List<T>> listType = new TypeToken<List<T>>() {
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
        };

        return listType.where(elementType, entityType);
    }

}
