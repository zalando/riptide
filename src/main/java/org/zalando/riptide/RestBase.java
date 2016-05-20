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

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriTemplateHandler;

abstract class RestBase<D> {

    protected final Router router = new Router();

    protected abstract <T> D execute(HttpMethod method, URI url, HttpEntity<T> entity);

    protected abstract UriTemplateHandler getUriTemplateHandler();

    public RestWithURL<D> withUrl(final String uriTemplate, final Object... uriVariables) {
        return new RestWithURL<>(this, getUriTemplateHandler().expand(uriTemplate, uriVariables));
    }

    public RestWithURL<D> withUrl(final String uriTemplate, final Map<String, ?> uriVariables) {
        return new RestWithURL<>(this, getUriTemplateHandler().expand(uriTemplate, uriVariables));
    }

    public D execute(final HttpMethod method, final URI url) {
        return execute(method, url, HttpEntity.EMPTY);
    }

    public D execute(final HttpMethod method, final URI url, final HttpHeaders headers) {
        return execute(method, url, new HttpEntity<>(headers));
    }

    public D execute(final HttpMethod method, final URI url, final Object body) {
        return execute(method, url, new HttpEntity<>(body));
    }

    public D execute(final HttpMethod method, final URI url, final HttpHeaders headers, final Object body) {
        return execute(method, url, new HttpEntity<>(body, headers));
    }
}
