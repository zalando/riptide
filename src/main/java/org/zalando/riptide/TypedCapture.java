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

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

import static java.util.Optional.empty;

@Immutable
final class TypedCapture<T> implements Capture {

    private final Optional<T> value;
    private final TypeToken<T> type;

    TypedCapture(final Optional<T> value, final TypeToken<T> type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public boolean has(TypeToken<?> other) {
        return other.isAssignableFrom(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O> Optional<O> opt(TypeToken<O> type) {
        return has(type) ? value.map(v -> (O) v) : empty();
    }

}
