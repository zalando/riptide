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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;

public abstract class Dispatcher {

    @SafeVarargs
    public final <A> CompletableFuture<Void> dispatch(final Navigator<A> selector, final Binding<A>... bindings) throws IOException {
        return dispatch(selector, asList(bindings));
    }

    public final <A> CompletableFuture<Void> dispatch(final Navigator<A> selector, final List<Binding<A>> bindings) throws IOException {
        return dispatch(RoutingTree.dispatch(selector, bindings));
    }

    public abstract <A> CompletableFuture<Void> dispatch(final RoutingTree<A> tree) throws IOException;

}
