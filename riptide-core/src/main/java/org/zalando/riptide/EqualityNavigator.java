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
import java.util.Optional;

public interface EqualityNavigator<A> extends Navigator<A> {

    /**
     * Retrieves an attribute from the given response
     *
     * @param response the incoming response
     * @return an attribute based on the response which is then used to select the correct binding
     * @throws IOException if accessing the response failed
     */
    @Nullable
    A attributeOf(final ClientHttpResponse response) throws IOException;

    @Override
    default Optional<Route> navigate(final ClientHttpResponse response, final RoutingTree<A> tree) throws IOException {
        @Nullable final A attribute = attributeOf(response);
        return navigate(attribute, tree);
    }

    default Optional<Route> navigate(@Nullable final A attribute, final RoutingTree<A> tree) throws IOException {
        return attribute == null ? tree.getWildcard() : tree.get(attribute);
    }

}
