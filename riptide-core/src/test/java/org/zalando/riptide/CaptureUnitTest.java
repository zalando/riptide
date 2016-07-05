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

import org.junit.Test;

import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.zalando.riptide.Capture.listOf;

public class CaptureUnitTest {

    @Test
    public void shouldNotRetrieveNullOnCaptured() {
        final Capture unit = Capture.valueOf(null);

        assertThat(unit.as(String.class), is(empty()));
    }

    @Test
    public void shouldRetrieveCaptured() {
        final Capture unit = Capture.valueOf("");

        assertThat(unit.as(String.class), is(not(empty())));
    }

    @Test
    public void shouldRetrieveTypedCapture() {
        final Capture unit = Capture.valueOf(newArrayList());

        assertThat(unit.as(listOf(String.class)), is(not(empty())));
    }

    @Test(expected = NoSuchElementException.class)
    public void shouldThrowOnRetrievingNull() {
        final Capture unit = Capture.valueOf(null);

        unit.to(String.class);
    }

}
