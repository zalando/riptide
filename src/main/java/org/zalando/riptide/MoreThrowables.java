package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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

import com.google.gag.annotation.remark.Booyah;
import com.google.gag.annotation.remark.Hack;

import java.util.Objects;

final class MoreThrowables {

    /**
     * Throws any checked exception without the need to declare it in the
     * throws clause.
     *
     * @param throwable the throwable to throw
     * @return never, this method <strong>always</strong> throws an exception
     * @see <a href="http://blog.jayway.com/2010/01/29/sneaky-throw">blog.jayway.com/2010/01/29/sneaky-throw</a>
     */
    @Hack
    @Booyah
    static RuntimeException sneakyThrow(Throwable throwable) {
        MoreThrowables.<RuntimeException>doSneakyThrow(throwable);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doSneakyThrow(Throwable throwable) throws T {
        throw (T) Objects.requireNonNull(throwable, "Throwable");
    }

}
