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

import org.springframework.http.HttpMethod;

import java.net.URI;

abstract class RestClient {

    public final Requester get(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.GET, urlTemplate, urlVariables);
    }

    public final Requester get(final URI url) {
        return execute(HttpMethod.GET, url);
    }

    public final Requester head(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.HEAD, urlTemplate, urlVariables);
    }

    public final Requester head(final URI url) {
        return execute(HttpMethod.HEAD, url);
    }

    public final Requester post(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.POST, urlTemplate, urlVariables);
    }

    public final Requester post(final URI url) {
        return execute(HttpMethod.POST, url);
    }

    public final Requester put(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PUT, urlTemplate, urlVariables);
    }

    public final Requester put(final URI url) {
        return execute(HttpMethod.PUT, url);
    }

    public final Requester patch(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.PATCH, urlTemplate, urlVariables);
    }

    public final Requester patch(final URI url) {
        return execute(HttpMethod.PATCH, url);
    }

    public final Requester delete(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.DELETE, urlTemplate, urlVariables);
    }

    public final Requester delete(final URI url) {
        return execute(HttpMethod.DELETE, url);
    }

    public final Requester options(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.OPTIONS, urlTemplate, urlVariables);
    }

    public final Requester options(final URI url) {
        return execute(HttpMethod.OPTIONS, url);
    }

    public final Requester trace(final String urlTemplate, final Object... urlVariables) {
        return execute(HttpMethod.TRACE, urlTemplate, urlVariables);
    }

    public final Requester trace(final URI url) {
        return execute(HttpMethod.TRACE, url);
    }

    protected abstract Requester execute(final HttpMethod method, final String urlTemplate,
            final Object... urlVariables);

    protected abstract Requester execute(final HttpMethod method, final URI url);

}
