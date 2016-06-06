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

import com.google.common.collect.Lists;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.annotation.Nullable;

public abstract class Requester<R> extends Dispatcher<R> {

    private final HttpHeaders headers = new HttpHeaders();

    public final Requester<R> header(final String name, final String value) {
        headers.add(name, value);
        return this;
    }

    public final Requester<R> headers(final HttpHeaders headers) {
        this.headers.putAll(headers);
        return this;
    }

    public final Requester<R> accept(final MediaType acceptableMediaType, final MediaType... acceptableMediaTypes) {
        headers.setAccept(Lists.asList(acceptableMediaType, acceptableMediaTypes));
        return this;
    }

    public final Requester<R> contentType(final MediaType contentType) {
        headers.setContentType(contentType);
        return this;
    }

    public final <T> Dispatcher<R> body(final T body) {
        return execute(headers, body);
    }

    @Override
    public final <A> R dispatch(final RoutingTree<A> tree) {
        return execute(headers, null).dispatch(tree);
    }

    protected abstract <T> Dispatcher<R> execute(final HttpHeaders headers, final @Nullable T body);

}
