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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public final class ThrowingFunctionTest {

    private final ThrowingFunction<String, String> appendA = s -> s + "A";
    private final ThrowingFunction<String, String> appendB = s -> s + "B";

    @Test
    public void shouldCompose() throws Exception {
        final String actual = appendA.compose(appendB).apply("");

        assertThat(actual, is("BA"));
    }

    @Test
    public void shouldComposeInReverse() throws Exception {
        final String actual = appendB.compose(appendA).apply("");

        assertThat(actual, is("AB"));
    }

    @Test
    public void shouldCombineWithAndThen() throws Exception {
        final String actual = appendA.andThen(appendB).apply("");

        assertThat(actual, is("AB"));
    }

    @Test
    public void shouldCombineWithAndThenInReverse() throws Exception {
        final String actual = appendB.andThen(appendA).apply("");

        assertThat(actual, is("BA"));
    }

}