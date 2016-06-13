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
import java.util.Map;
import java.util.Optional;

@Immutable
public interface Capture {

    default boolean has(final Class<?> type) {
        return has(TypeToken.of(type));
    }

    boolean has(final TypeToken<?> type);

    default <T> Optional<T> as(final Class<T> type) {
        return as(TypeToken.of(type));
    }

    <T> Optional<T> as(final TypeToken<T> type);

    default <T> T to(final Class<T> type) {
        return to(TypeToken.of(type));
    }

    default <T> T to(final TypeToken<T> type) {
        return as(type).get();
    }

    static Capture none() {
        return valueOf(null);
    }

    static <T> Capture valueOf(@Nullable final T value) {
        return new RawCapture(Optional.ofNullable(value));
    }

    static <T> Capture valueOf(@Nullable final T value, final Class<T> type) {
        return valueOf(value, TypeToken.of(type));
    }

    static <T> Capture valueOf(@Nullable final T value, final TypeToken<T> type) {
        return new TypedCapture<>(Optional.ofNullable(value), type);
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

    static <K, V> TypeToken<Map<K, V>> mapOf(final Class<K> keyType, final Class<V> valueType) {
        return mapOf(TypeToken.of(keyType), TypeToken.of(valueType));
    }

    static <K, V> TypeToken<Map<K, V>> mapOf(final TypeToken<K> keyType, final TypeToken<V> valueType) {
        final TypeToken<Map<K, V>> mapType = new TypeToken<Map<K, V>>() {
        };

        final TypeParameter<K> keyParameter = new TypeParameter<K>() {
        };

        final TypeParameter<V> valueParameter = new TypeParameter<V>() {
        };

        return mapType.where(keyParameter, keyType).where(valueParameter, valueType);

    }


}
