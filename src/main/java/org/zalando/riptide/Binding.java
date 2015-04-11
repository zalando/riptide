package org.zalando.riptide;

/*
 * #%L
 * rest-dispatcher
 * %%
 * Copyright (C) 2015 Zalando SE
 * %%
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
 * #L%
 */

import org.springframework.core.ParameterizedTypeReference;

import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Binding<A, I, O> implements Function<I, O> {

    private final A attribute;
    private final Type type;
    private final Function<I, O> mapper;

    Binding(A attribute, Type type, Function<I, O> mapper) {
        this.attribute = attribute;
        this.type = type;
        this.mapper = mapper;
    }

    public A getAttribute() {
        return attribute;
    }

    public Type getType() {
        return type;
    }

    @Override
    public O apply(I i) {
        return mapper.apply(i);
    }

    private static <I> Function<I, Void> asFunction(Consumer<I> consumer) {
        return input -> {
            consumer.accept(input);
            return null;
        };
    }

    public static <A, I> Binding<A, I, Void> consume(A attribute, ParameterizedTypeReference<I> type, Consumer<I> consumer) {
        return consume(attribute, type.getType(), consumer);
    }

    public static <A, I> Binding<A, I, Void> consume(A attribute, Type type, Consumer<I> consumer) {
        return map(attribute, type, asFunction(consumer));
    }

    public static <A, I, O> Binding<A, I, O> map(A attribute, ParameterizedTypeReference<I> type, Function<I, O> mapper) {
        return map(attribute, type.getType(), mapper);
    }

    public static <A, I, O> Binding<A, I, O> map(A attribute, Type type, Function<I, O> mapper) {
        return new Binding<>(attribute, type, mapper);
    }
    
}
