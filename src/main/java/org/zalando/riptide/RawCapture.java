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
final class RawCapture implements Capture {

    private final Optional<Object> value;

    RawCapture(final Optional<Object> value) {
        this.value = value;
    }

    @Override
    public boolean has(TypeToken<?> other) {
        return value.map(Object::getClass)
                .filter(other::isSupertypeOf)
                .isPresent();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O> Optional<O> as(TypeToken<O> type) {
        return has(type) ? value.map(v -> (O) v) : empty();
    }

}
