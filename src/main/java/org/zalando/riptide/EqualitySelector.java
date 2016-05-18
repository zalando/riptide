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

import com.google.common.collect.Maps;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface EqualitySelector<A> extends Selector<A> {


    /**
     * Attempts to find a matching binding for the given attribute. Defaults to a direct map lookup.
     *
     * @inheritDoc
     */
    @Override
    default Binding<A> select(final ClientHttpResponse response, final Collection<Binding<A>> bindings) throws IOException {
        final A attribute = attributeOf(response);
        return select(attribute, bindings);
    }

    /**
     * Retrieves an attribute from the given response
     *
     * @param response the incoming response
     * @return an attribute based on the response which is then used to select the correct binding
     * @throws IOException if accessing the response failed
     */
    @Nullable
    A attributeOf(final ClientHttpResponse response) throws IOException;

    default Binding<A> select(@Nullable A attribute, Collection<Binding<A>> bindings) {
        final Map<A, Binding<A>> index = Maps.uniqueIndex(bindings, Binding::getAttribute);
        return select(attribute, index);
    }

    default Binding<A> select(@Nullable A attribute, Map<A, Binding<A>> index) {
        return index.get(attribute);
    }

}
