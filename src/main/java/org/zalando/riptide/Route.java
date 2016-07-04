package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import java.io.IOException;

import static org.zalando.riptide.Capture.none;

@FunctionalInterface
public interface Route {

    Capture execute(final ClientHttpResponse response, final MessageReader reader) throws IOException;

    static Route capture() {
        return (response, reader) ->
                Capture.valueOf(response);
    }

    static Route capture(final ThrowingFunction<ClientHttpResponse, ?> function) {
        return (response, reader) ->
                Capture.valueOf(function.apply(response));
    }

    static Route call(final ThrowingRunnable consumer) {
        return (response, reader) -> {
            consumer.run();
            return none();
        };
    }

    static Route call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return (response, reader) -> {
            consumer.accept(response);
            return none();
        };
    }

    // TODO ResponseEntity<T> version of this?
    static <I> Route capture(final Class<I> type) {
        return capture(TypeToken.of(type));
    }

    static <I> Route capture(final TypeToken<I> type) {
        return (response, reader) ->
                Capture.valueOf(reader.readEntity(type, response));
    }

    static <I, T> Route capture(final Class<I> type, final EntityFunction<I, T> function) {
        return capture(TypeToken.of(type), function);
    }

    static <I, T> Route capture(final TypeToken<I> type, final EntityFunction<I, T> function) {
        return (response, reader) ->
                Capture.valueOf(function.apply(reader.readEntity(type, response)));
    }

    static <I, T> Route capture(final Class<I> type, final ResponseEntityFunction<I, T> function) {
        return capture(TypeToken.of(type), function);
    }

    static <I, T> Route capture(final TypeToken<I> type, final ResponseEntityFunction<I, T> function) {
        return (response, reader) ->
                Capture.valueOf(function.apply(reader.readResponseEntity(type, response)));
    }

    static <I> Route call(final Class<I> type, final EntityConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final EntityConsumer<I> consumer) {
        return (response, reader) -> {
            consumer.accept(reader.readEntity(type, response));
            return none();
        };
    }

    static <I> Route call(final Class<I> type, final ResponseEntityConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final ResponseEntityConsumer<I> consumer) {
        return (response, reader) -> {
            consumer.accept(reader.readResponseEntity(type, response));
            return none();
        };
    }

    @SafeVarargs
    static <B> Route dispatch(final Navigator<B> navigator, final Binding<B>... bindings) {
        return RoutingTree.create(navigator, bindings);
    }

    @SafeVarargs
    static <B> Route dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final Navigator<B> navigator, final Binding<B>... bindings) {
        return dispatch(function, RoutingTree.create(navigator, bindings));
    }

    static <B> Route dispatch(final ThrowingFunction<ClientHttpResponse, ClientHttpResponse> function,
            final RoutingTree<B> tree) {
        return (response, reader) ->
                tree.execute(function.apply(response), reader);
    }

}
