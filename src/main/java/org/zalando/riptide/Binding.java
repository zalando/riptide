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
import org.springframework.http.client.ClientHttpResponse;

import java.util.function.Consumer;

public class Binding<A> {

    public static <A> DispatchableCondition<A> on(A attribute) {
        throw new UnsupportedOperationException();
    }

    public static <A, I> CallableCondition<A, I> on(A attribute, Class<I> type) {
        throw new UnsupportedOperationException();
    }

    public static <A> AnyCondition<A> any(Class<A> type) {
        throw new UnsupportedOperationException();
    }
    
    public static AnyCondition<HttpStatus> anyStatusCode() {
        return any(HttpStatus.class);
    }
    
    public static AnyCondition<HttpStatus.Series> anySeries() {
        return any(HttpStatus.Series.class);
    }
    
    public static AnyCondition<MediaType> anyContentType() {
        return any(MediaType.class);
    }

}
