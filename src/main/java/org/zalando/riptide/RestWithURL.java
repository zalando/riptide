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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public final class RestWithURL<D> {

    private final RestBase<D> rest;
    private final URI url;

    RestWithURL(final RestBase<D> rest, final URI url) {
        this.rest = rest;
        this.url = url;
    }

    public D execute(final HttpMethod method) {
        return rest.execute(method, url, HttpEntity.EMPTY);
    }

    public D execute(final HttpMethod method, final HttpHeaders headers) {
        return rest.execute(method, url, new HttpEntity<>(headers));
    }

    public D execute(final HttpMethod method, final Object body) {
        return rest.execute(method, url, new HttpEntity<>(body));
    }

    public D execute(final HttpMethod method, final HttpHeaders headers, final Object body) {
        return rest.execute(method, url, new HttpEntity<>(body, headers));
    }
}
