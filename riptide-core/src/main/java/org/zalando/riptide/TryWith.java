package org.zalando.riptide;

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

import java.io.Closeable;

public interface TryWith {

    static void tryWith(final Closeable closeable, final ThrowingRunnable consumer) throws Exception {
        try {
            consumer.run();
        } catch (Exception ex) {
            try {
                closeable.close();
            } catch (Exception cex) {
                ex.addSuppressed(cex);
            }
            throw ex;
        }
        closeable.close();
    }

}
