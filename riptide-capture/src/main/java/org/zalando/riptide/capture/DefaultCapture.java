package org.zalando.riptide.capture;

/*
 * ⁣​
 * Riptide: Capture
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

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultCapture<T> implements Capture<T> {

    private final AtomicReference<T> reference = new AtomicReference<>();

    @Override
    public void accept(final T input) throws Exception {
        reference.set(input);
    }

    @Override
    public T retrieve() throws NoSuchElementException {
        final T value = reference.get();

        if (value == null) {
            throw new NoSuchElementException("No value present");
        }

        return value;
    }

    @Override
    public ListenableFuture<T> adapt(final ListenableFuture<Void> future) {
        return new ListenableFutureAdapter<T, Void>(future) {
            @Override
            protected T adapt(final Void result) throws ExecutionException {
                return retrieve();
            }
        };
    }

}
