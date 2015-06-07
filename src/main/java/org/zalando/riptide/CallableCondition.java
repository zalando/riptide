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

import com.google.common.reflect.TypeToken;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class CallableCondition<A, I> implements Capturer<A> {

    private final A attribute;
    private final TypeToken<I> type;

    public CallableCondition(A attribute, TypeToken<I> type) {
        this.attribute = attribute;
        this.type = type;
    }

    public Binding<A> call(EntityConsumer<I> consumer) {
        return new Binding<A>() {
            @Override
            public Optional<A> getAttribute() {
                return Optional.of(attribute);
            }

            @Override
            public Object execute(ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {
                final I body = new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
                consumer.accept(body);
                return null;
            }
        };
    }

    public Binding<A> call(ResponseEntityConsumer<I> consumer) {
        return new Binding<A>() {
            @Override
            public Optional<A> getAttribute() {
                return Optional.of(attribute);
            }

            @Override
            public Object execute(ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {
                final I body = new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
                consumer.accept(new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode()));
                return null;
            }
        };
    }

    public Capturer<A> map(EntityFunction<I, ?> function) {
        return new Capturer<A>() {
            @Override
            public Binding<A> capture() {
                return new Binding<A>() {
                    @Override
                    public Optional<A> getAttribute() {
                        return Optional.of(attribute);
                    }

                    @Override
                    public Object execute(ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {
                        final I body = new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
                        return function.apply(body);                        
                    }
                };
            }
        };
    }

    public Capturer<A> map(ResponseEntityFunction<I, ?> function) {
        return new Capturer<A>() {
            @Override
            public Binding<A> capture() {
                return new Binding<A>() {
                    @Override
                    public Optional<A> getAttribute() {
                        return Optional.of(attribute);
                    }

                    @Override
                    public Object execute(ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {
                        final I body = new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
                        return function.apply(new ResponseEntity<>(body, response.getHeaders(), response.getStatusCode()));
                    }
                };
            }
        };
    }

    @Override
    public Binding<A> capture() {
        return new Binding<A>() {
            @Override
            public Optional<A> getAttribute() {
                return Optional.of(attribute);
            }

            @Override
            public Object execute(ClientHttpResponse response, List<HttpMessageConverter<?>> converters) throws IOException {
                return new HttpMessageConverterExtractor<I>(type.getType(), converters).extractData(response);
            }
        };
    }
    
}
