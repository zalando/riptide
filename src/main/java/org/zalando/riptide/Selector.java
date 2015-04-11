package org.zalando.riptide;

/*
 * #%L
 * rest-dispatcher
 * %%
 * Copyright (C) 2015 Zalando SE
 * %%
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
 * #L%
 */

import org.springframework.http.client.ClientHttpResponse;

import java.util.Map;
import java.util.Optional;

/**
 * A {@link Selector} can be used change the dispatching strategy. Its purpose is to select an attribute
 * of the response and find a binding for it. 
 * 
 * @param <A> generic response attribute type
 */
@FunctionalInterface
public interface Selector<A> {

    /**
     * Retrieves an attribute from the given response
     * 
     * @param response the incoming response
     * @return an attribute based on the response which is then used to select the correct binding
     */
    A attributeOf(ClientHttpResponse response);

    /**
     * Attempts to find a matching binding for the given attribute. Defaults to a direct map lookup.
     * 
     * @param attribute the previously selected attribute
     * @param bindings all bindings
     * @param <O> the generic output type parameter
     * @return an optional binding match, if found
     */
    default <O> Optional<Binding<A, ?, O>> select(A attribute, Map<A, Binding<A, ?, O>> bindings) {
        return Optional.ofNullable(bindings.get(attribute));
    }

}
