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

import org.zalando.fauxpas.ThrowingConsumer;
import org.zalando.riptide.Completion;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;

public interface Capture<T> extends ThrowingConsumer<T, RuntimeException> {

    @Nullable T retrieve() throws NoSuchElementException;

    Completion<T> adapt(final Completion<Void> future);

    static <T> Capture<T> empty() {
        return new DefaultCapture<>();
    }

}
