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

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface ClientHttpResponseConsumer {

    void accept(ClientHttpResponse clientHttpResponse) throws IOException;

    default ClientHttpResponseConsumer andThen(Consumer<? super ClientHttpResponse> after) {
        Objects.requireNonNull(after);
        return (ClientHttpResponse t) -> {
            accept(t);
            after.accept(t);
        };
    }

    default ClientHttpResponseConsumer andThen(ClientHttpResponseConsumer after) {
        Objects.requireNonNull(after);
        return (ClientHttpResponse t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
