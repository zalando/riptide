package org.zalando.riptide.tryit;

/*
 * ⁣​
 * Riptide: Core
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

import org.zalando.riptide.ThrowingConsumer;
import org.zalando.riptide.ThrowingFunction;
import org.zalando.riptide.ThrowingRunnable;
import org.zalando.riptide.ThrowingSupplier;

public interface TryWith {

    static void tryWith(final AutoCloseable closeable, final ThrowingRunnable runnable) throws Exception {
        try {
            runnable.run();
        } catch (Exception e) {
            try {
                closeable.close();
            } catch (Exception ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
        closeable.close();
    }

    static <T> void tryWith(final AutoCloseable closeable, final ThrowingConsumer<T> consumer, final T input)
            throws Exception {
        try {
            consumer.accept(input);
        } catch (Exception e) {
            try {
                closeable.close();
            } catch (Exception ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
        closeable.close();
    }

    static <T> T tryWith(final AutoCloseable closeable, final ThrowingSupplier<T> supplier) throws Exception {
        T value = null;
        try {
            value = supplier.get();
        } catch (Exception e) {
            try {
                closeable.close();
            } catch (Exception ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
        closeable.close();
        return value;
    }

    static <R, T> T tryWith(final AutoCloseable closeable, final ThrowingFunction<R, T> function, final R input)
            throws Exception {
        T value = null;
        try {
            value = function.apply(input);
        } catch (Exception e) {
            try {
                closeable.close();
            } catch (Exception ce) {
                e.addSuppressed(ce);
            }
            throw e;
        }
        closeable.close();
        return value;
    }
}
