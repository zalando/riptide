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
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public final class Dispatcher {

    private final RestTemplate template;
    private final HttpMethod method;
    private final URI url;
    private final RequestCallback request;
    private final Propagator propagator = new Propagator();

    public Dispatcher(RestTemplate template, HttpMethod method, URI url, RequestCallback request) {
        this.template = template;
        this.method = method;
        this.url = url;
        this.request = request;
    }

    @SafeVarargs
    public final <A> Retriever dispatch(Selector<A> selector, Binding<A>... bindings) {
        final Object value = template.execute(url, method, request, response -> {
            return propagator.propagate(response, template.getMessageConverters(), selector, bindings);
        });

        return new Retriever(value);
    }

}
