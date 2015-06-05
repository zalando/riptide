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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

public class Rest {

    private final RestTemplate template;

    private Rest(RestTemplate template) {
        this.template = template;
    }

    Dispatcher execute(HttpMethod method, URI url) {
        return null;
    }

    Dispatcher execute(HttpMethod method, URI url, Object entity) {
        return null;
    }

    Dispatcher execute(HttpMethod method, URI url, Object entity, HttpHeaders headers) {
        return null;
    }

    Dispatcher execute(HttpMethod method, URI url, HttpHeaders headers) {
        return null;
    }

    public static Rest create(RestTemplate template) {
        return new Rest(template);
    }

}
