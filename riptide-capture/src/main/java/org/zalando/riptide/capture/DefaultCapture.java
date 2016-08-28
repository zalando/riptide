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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultCapture<T> implements Capture<T> {

    private final AtomicReference<T> reference = new AtomicReference<>();
    private final AtomicBoolean present = new AtomicBoolean();

    @Override
    public void tryAccept(@Nullable final T input) {
        reference.set(input);
        present.set(true);
    }

    @Override
    public T apply(final Void result) {
        checkPresent();
        return reference.get();
    }

    private void checkPresent() {
        if (present.get()) {
            return;
        }

        throw new NoSuchElementException("No value present");
    }

}
