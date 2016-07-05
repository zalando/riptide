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
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.util.List;

public final class PartialBinding<A> {

    private final A attribute;

    PartialBinding(@Nullable final A attribute) {
        this.attribute = attribute;
    }

    public Binding<A> call(final ThrowingRunnable consumer) {
        return call(Route.call(consumer));
    }

    public Binding<A> call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return call(Route.call(consumer));
    }

    public <I> Binding<A> call(final Class<I> type, final ThrowingConsumer<I> consumer) {
        return call(Route.call(type, consumer));
    }

    public <I> Binding<A> call(final TypeToken<I> type, final ThrowingConsumer<I> consumer) {
        return call(Route.call(type, consumer));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final Navigator<B> navigator, final Binding<B>... bindings) {
        return call(RoutingTree.dispatch(navigator, bindings));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final Navigator<B> navigator, final Binding<B>... bindings) {
        return call(Route.dispatch(function, navigator, bindings));
    }

    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final RoutingTree<B> tree) {
        return call(Route.dispatch(function, tree));
    }

    public Binding<A> call(final Route route) {
        return Binding.create(attribute, route);
    }

    public static <T> TypeToken<List<T>> listOf(final Class<T> entityType) {
        return listOf(TypeToken.of(entityType));
    }

    public static <T> TypeToken<List<T>> listOf(final TypeToken<T> entityType) {
        final TypeToken<List<T>> listType = new TypeToken<List<T>>() {
        };

        final TypeParameter<T> elementType = new TypeParameter<T>() {
        };

        return listType.where(elementType, entityType);
    }

}
