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

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultCapture<T> implements Capture<T> {

    /**
     * A reference holding the captured value. It can represent three different states:
     * <ol>
     *     <li>{@code null}: not captured</li>
     *     <li>{@code Optional.empty()}: captured null</li>
     *     <li>{@code Optional.of(..)}: captured non-null</li>
     * </ol>
     */
    private final AtomicReference<Optional<T>> reference = new AtomicReference<>();

    @Override
    public void tryAccept(@Nullable final T input) {
        reference.compareAndSet(null, Optional.ofNullable(input));
    }

    @Override
    public T apply(final Void result) {
        final Optional<T> value = reference.get();
        checkPresent(value);
        return value.orElse(null);
    }

    private void checkPresent(@Nullable final Optional<T> value) {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
    }

}
