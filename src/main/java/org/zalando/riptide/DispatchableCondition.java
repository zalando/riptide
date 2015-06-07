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
import org.springframework.http.converter.HttpMessageConverter;

import java.util.Optional;
import java.util.function.Consumer;

public final class DispatchableCondition<A> {
    
    private final Optional<A> attribute;

    public DispatchableCondition(Optional<A> attribute) {
        this.attribute = attribute;
    }

    public Binding<A> call(Consumer<ClientHttpResponse> consumer) {
        return new Binding<A>() {
            @Override
            public A getAttribute() {
                return attribute.orElse(null);
            }

            @Override
            public Object execute(ClientHttpResponse response, Iterable<HttpMessageConverter<?>> converters) {
                consumer.accept(response);
                return null;
            }
        };
    }

    @SafeVarargs
    public final <B> Binding<A> dispatch(Selector<B> selector, Binding<B>... binding) {
        throw new UnsupportedOperationException();
    }

}
