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

import com.google.common.reflect.TypeToken;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

@FunctionalInterface
public interface Route {

    void execute(final ClientHttpResponse response, final MessageReader reader) throws IOException;

    static Route call(final ThrowingRunnable consumer) {
        return (response, reader) ->
                consumer.run();
    }

    static Route call(final ThrowingConsumer<ClientHttpResponse> consumer) {
        return (response, reader) ->
                consumer.accept(response);
    }

    static <I> Route call(final Class<I> type, final ThrowingConsumer<I> consumer) {
        return call(TypeToken.of(type), consumer);
    }

    static <I> Route call(final TypeToken<I> type, final ThrowingConsumer<I> consumer) {
        return (response, reader) ->
                consumer.accept(reader.read(type, response));
    }

}
