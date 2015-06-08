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

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

import static java.util.Arrays.asList;

public final class Dispatcher {

    private final RestTemplate template;
    private final HttpMethod method;
    private final URI url;
    private final RequestCallback callback;
    private final Propagator propagator = new Propagator();

    Dispatcher(RestTemplate template, HttpMethod method, URI url, RequestCallback callback) {
        this.template = template;
        this.method = method;
        this.url = url;
        this.callback = callback;
    }

    @SafeVarargs
    public final <A> Retriever dispatch(Selector<A> selector, Binding<A>... bindings) {
        final Object value = template.execute(url, method, callback, response -> {
            final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
            return propagator.propagate(response, converters, selector, asList(bindings));
        });

        return new Retriever(value);
    }

}
