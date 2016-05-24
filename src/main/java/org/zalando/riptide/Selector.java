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

import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * A {@link Selector} can be used change the dispatching strategy. Its purpose is to find a binding for a given
 * response.
 *
 * @param <A> generic binding attribute type
 */
public interface Selector<A> {

    /**
     * Attempts to find a matching binding for the given response.
     *
     * @param response the received response
     * @param bindings  a map of all bindings
     * @return an optional binding match, if found
     */
    @Nullable
    Binding<A> select(final ClientHttpResponse response, final Map<A, Binding<A>> bindings) throws IOException;

}
