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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class Selectors {

    private Selectors() {

    }

    /**
     * A {@link Selector} that selects a binding based on the response's status code series
     *
     * @return an HTTP status code series selector
     * @see org.springframework.http.HttpStatus.Series
     */
    public static Selector<HttpStatus.Series> series() {
        return new SeriesSelector();
    }

    /**
     * A {@link Selector} that selects a binding based on the response's status code.
     *
     * @return an HTTP status code selector
     * @see HttpStatus
     */
    public static Selector<HttpStatus> statusCode() {
        return new StatusCodeSelector();
    }

    /**
     * A {@link Selector} that selects the best binding based on the response's content type.
     *
     * @return a Content-Type selector
     * @see MediaType
     */
    public static Selector<MediaType> contentType() {
        return new ContentTypeSelector();
    }

}
