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

import com.google.common.reflect.TypeToken;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
interface Capture<T> {

    Optional<T> getValue();

    boolean isAssignableTo(final TypeToken<?> otherType);

    static <T> Capture<T> captured(@Nullable final T value, final Class<T> type) {
        return captured(value, TypeToken.of(type));
    }

    static <T> Capture<T> captured(@Nullable final T value, final TypeToken<T> type) {
        return new TypedCapture<>(Optional.ofNullable(value), type);
    }

    static <T> Capture<T> wrapNothing() {
        return captured(null);
    }

    static <T> Capture<T> captured(@Nullable final T value) {
        return new RawCapture<>(Optional.ofNullable(value));
    }

}
