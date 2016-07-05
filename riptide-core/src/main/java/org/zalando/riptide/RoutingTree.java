package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 - 2016 Zalando SE
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;

public interface RoutingTree<A> extends Route {

    Set<A> keySet();

    Optional<Route> get(final A attribute);

    Optional<Route> getWildcard();

    default RoutingTree<A> merge(final Binding<A> binding) {
        return merge(Collections.singletonList(binding));
    }

    RoutingTree<A> merge(final List<Binding<A>> bindings);

    @SafeVarargs
    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final Binding<A>... bindings) {
        return dispatch(navigator, asList(bindings));
    }

    static <A> RoutingTree<A> dispatch(final Navigator<A> navigator, final List<Binding<A>> bindings) {
        return new DefaultRoutingTree<>(navigator, bindings);
    }

}
