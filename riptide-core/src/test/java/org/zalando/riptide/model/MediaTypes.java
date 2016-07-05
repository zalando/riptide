package org.zalando.riptide.model;

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

import org.springframework.http.MediaType;

import static org.springframework.http.MediaType.parseMediaType;

public final class MediaTypes {

    public static final MediaType SUCCESS = parseMediaType("application/success+json");
    public static final MediaType SUCCESS_V1 = parseMediaType("application/success+json;version=1");
    public static final MediaType SUCCESS_V2 = parseMediaType("application/success+json;version=2");
    public static final MediaType ERROR = parseMediaType("application/vnd.error+json");
    public static final MediaType PROBLEM = parseMediaType("application/problem+json");

}
