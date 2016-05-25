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

import javax.annotation.Nullable;

public final class Binding<A> {

    private final A attribute;
    private final Executor executor;

    private Binding(@Nullable final A attribute, final Executor executor) {
        this.attribute = attribute;
        this.executor = executor;
    }

    @Nullable
    A getAttribute() {
        return attribute;
    }

    public Executor getExecutor() {
        return executor;
    }

    static <A> Binding<A> create(@Nullable final A attribute, final Executor executor) {
        return new Binding<>(attribute, executor);
    }

}
