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
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;

public final class PartialBinding<A> {

    private final A attribute;

    PartialBinding(@Nullable final A attribute) {
        this.attribute = attribute;
    }

    public <I> Binding<A> stream(final Class<I> type) {
        return bind(Route.stream(TypeToken.of(type)));
    }

    public <I> Binding<A> stream(final TypeToken<I> type) {
        return bind(Route.stream(type));
    }

    public Binding<A> capture() {
        return bind(Route.capture());
    }

    public Binding<A> capture(final ThrowingFunction<ClientHttpResponse, ?> function) {
        return bind(Route.capture(function));
    }

    public Binding<A> call(final ThrowingRunnable consumer) {
        return bind(Route.call(consumer));
    }

    public Binding<A> call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return bind(Route.call(consumer));
    }

    // TODO ResponseEntity<T> version of this?
    public <I> Binding<A> capture(final Class<I> type) {
        return bind(Route.capture(type));
    }

    public <I> Binding<A> capture(final TypeToken<I> type) {
        return bind(Route.capture(type));
    }

    public <I, T> Binding<A> capture(final Class<I> type, final EntityFunction<I, T> function) {
        return bind(Route.capture(type, function));
    }

    public <I, T> Binding<A> capture(final TypeToken<I> type, final EntityFunction<I, T> function) {
        return bind(Route.capture(type, function));
    }

    public <I, T> Binding<A> capture(final Class<I> type, final ResponseEntityFunction<I, T> function) {
        return bind(Route.capture(type, function));
    }

    public <I, T> Binding<A> capture(final TypeToken<I> type, final ResponseEntityFunction<I, T> function) {
        return bind(Route.capture(type, function));
    }

    public <I> Binding<A> call(final Class<I> type, final EntityConsumer<I> consumer) {
        return bind(Route.call(type, consumer));
    }

    public <I> Binding<A> call(final TypeToken<I> type, final EntityConsumer<I> consumer) {
        return bind(Route.call(type, consumer));
    }

    public <I> Binding<A> call(final Class<I> type, final ResponseEntityConsumer<I> consumer) {
        return bind(Route.call(type, consumer));
    }

    public <I> Binding<A> call(final TypeToken<I> type, final ResponseEntityConsumer<I> consumer) {
        return bind(Route.call(type, consumer));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final Navigator<B> navigator, final Binding<B>... bindings) {
        return bind(Route.dispatch(navigator, bindings));
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final Navigator<B> navigator, final Binding<B>... bindings) {
        return bind(Route.dispatch(function, navigator, bindings));
    }

    public final <B> Binding<A> dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final RoutingTree<B> tree) {
        return bind(Route.dispatch(function, tree));
    }

    public Binding<A> bind(final Route route) {
        return Binding.create(attribute, route);
    }

}
