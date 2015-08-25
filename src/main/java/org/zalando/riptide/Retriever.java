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

import java.util.Optional;

public final class Retriever {

    private final Capture<?> capture;

    public Retriever(final Capture<?> capture) {
        this.capture = capture;
    }

    public <T> Optional<T> retrieve(final Class<T> type) {
        return retrieve(TypeToken.of(type));
    }

    public <T> Optional<T> retrieve(final TypeToken<T> type) {
        if (capture.isAssignableTo(type)) {
            return capture.getValue().map(this::cast);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(final Object value) {
        return (T) value;
    }

    // TODO feels weird that this may return true for empty captures, but client's can at least differentiate now
    public boolean hasRetrieved(final Class<?> type) {
        return hasRetrieved(TypeToken.of(type));
    }

    public boolean hasRetrieved(final TypeToken<?> type) {
        return capture.isAssignableTo(type);
    }

}
