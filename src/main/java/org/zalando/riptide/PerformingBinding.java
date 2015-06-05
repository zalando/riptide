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

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class PerformingBinding<A, I> extends Binding<A> {

    public Binding<A> call(EntityConsumer<I> consumer) {
        throw new UnsupportedOperationException();
    }

    public Binding<A> call(ResponseConsumer consumer) {
        throw new UnsupportedOperationException();
    }

}
