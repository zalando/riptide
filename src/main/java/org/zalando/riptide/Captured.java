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

class Captured {

    public static Captured wrap(@Nullable final Object value, final TypeToken<?> type) {
        return new TypedCaptured(value, type);
    }

    public static Captured wrap(@Nullable final Object value, final Class<?> type) {
        return wrap(value, TypeToken.of(type));
    }

    public static Captured wrap(@Nullable final Object value) {
        return new Captured(value);
    }

    public static Captured wrapNothing() {
        return wrap(null);
    }

    private final Object value;

    Captured(@Nullable final Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public boolean isAssignableTo(final TypeToken<?> otherType) {
        return value != null && otherType.isAssignableFrom(value.getClass());
    }
}
