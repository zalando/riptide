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

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;

public final class Selectors {

    Selectors() {
        // package private so we can trick code coverage
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status code series
     *
     * @return an HTTP status code series selector
     * @see Series
     */
    public static Navigator<Series> series() {
        return SeriesNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status.
     *
     * @return an HTTP status selector
     * @see HttpStatus
     * @see #statusCode()
     */
    public static Navigator<HttpStatus> status() {
        return StatusNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status code.
     *
     * @return an HTTP status code selector
     * @see HttpStatus
     * @see #status()
     */
    public static Navigator<Integer> statusCode() {
        return StatusCodeNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's reason phrase.
     *
     * Be aware that this, even though it's standardized, could be changed by servers.
     *
     * @return an HTTP reason phrase selector
     * @see HttpStatus#getReasonPhrase()
     * @see #status()
     * @see #statusCode()
     */
    public static Navigator<String> reasonPhrase() {
        return ReasonPhraseNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects the best binding based on the response's content type.
     *
     * @return a Content-Type selector
     * @see MediaType
     */
    public static Navigator<MediaType> contentType() {
        return ContentTypeNavigator.INSTANCE;
    }

    /**
     * A {@link BinaryNavigator} that selects a binding based on whether the {@code Location} header is present
     * and has the same field value as the {@code Content-Location} header.
     *
     * <pre>
     * For a 201 (Created) response to a state-changing method, a
     * Content-Location field-value that is identical to the Location
     * field-value indicates that this payload is a current
     * representation of the newly created resource.
     * </pre>
     * 
     * @return a Content-Location header selector
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">RFC 7231 Section 3.1.4.2. Content-Location</a>
     */
    public static Navigator<Boolean> isCurrentRepresentation() {
        return CurrentRepresentationNavigator.INSTANCE;
    }

}
