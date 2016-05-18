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

import com.google.common.reflect.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class Conditions {

    Conditions() {
        // package private so we can trick code coverage
    }

    public static <A> Condition<A> on(final A attribute) {
        return new Condition<>(attribute);
    }

    /**
     * Creates an <i>any</i> condition for the given type. Note that this method is meant to be
     * used as a base for specialized factory methods, e.g. like {@link #anyStatus()}.
     *
     * @param type attribute type
     * @param <A>  generic attribute type
     * @return an any condition on the given attribute type
     * @see #any(TypeToken)
     * @see #anySeries()
     * @see #anyStatus()
     * @see #anyStatusCode()
     * @see #anyContentType()
     */
    public static <A> Condition<A> any(final Class<A> type) {
        return any(TypeToken.of(type));
    }
    
    /**
     * Creates a wildcard condition for the given type. Note that this method is meant to be
     * used as a base for specialized factory methods, e.g. like {@link #anyStatus()}.
     * 
     * @param type attribute type
     * @param <A> generic attribute type
     * @return an any condition on the given attribute type
     * @see #any(Class) 
     * @see #anySeries() 
     * @see #anyStatus() 
     * @see #anyStatusCode() 
     * @see #anyContentType() 
     */
    public static <A> Condition<A> any(@SuppressWarnings("UnusedParameters") final TypeToken<A> type) {
        return new Condition<>(null);
    }

    public static Condition<HttpStatus.Series> anySeries() {
        return any(HttpStatus.Series.class);
    }

    public static Condition<HttpStatus> anyStatus() {
        return any(HttpStatus.class);
    }
    
    public static Condition<Integer> anyStatusCode() {
        return any(Integer.class);
    }

    public static Condition<MediaType> anyContentType() {
        return any(MediaType.class);
    }

}
