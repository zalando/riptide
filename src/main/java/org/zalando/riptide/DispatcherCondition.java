package org.zalando.riptide;

/*
 * ⁣​
 * riptide
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

import org.springframework.http.client.ClientHttpResponse;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Arrays.asList;

public final class DispatcherCondition<A> {
    
    private final Router router = new Router();

    private final Optional<A> attribute;

    public DispatcherCondition(Optional<A> attribute) {
        this.attribute = attribute;
    }

    public Binding<A> call(Consumer<ClientHttpResponse> consumer) {
        return Binding.create(attribute, (response, converters) -> {
            consumer.accept(response);
            return null;
        });
    }

    public Capturer<A> map(Function<ClientHttpResponse, ?> function) {
        return () -> Binding.create(attribute, (response, converters) -> function.apply(response));
    }

    public Binding<A> capture() {
        return Binding.create(attribute, (response, converters) -> response);
    }

    /**
     * 
     * @param selector
     * @param bindings
     * @param <B>
     * @return
     * @throws UnsupportedResponseException
     */
    @SafeVarargs
    public final <B> Binding<A> dispatch(Selector<B> selector, Binding<B>... bindings) {
        return Binding.create(attribute, (response, converters) ->
                router.route(response, converters, selector, asList(bindings)));
    }

}
