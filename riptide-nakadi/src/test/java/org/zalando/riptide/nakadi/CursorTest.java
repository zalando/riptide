package org.zalando.riptide.nakadi;

/*
 * ⁣​
 * Riptide: Nakadi
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;

public class CursorTest extends ObjectTheory {

    private static final String PARTITION = "0";
    private static final String OFFSET = "1";

    @DataPoints
    public static Cursor[] data = {
            null,
            new Cursor(null, null),
            new Cursor(PARTITION, null),
            new Cursor(PARTITION, null),
            new Cursor(null, OFFSET),
            new Cursor(null, OFFSET)
    };

    @Test
    public void getter() {
        Cursor cursor = new Cursor(PARTITION, OFFSET);
        assertThat(cursor.getPartition(), is(equalTo(PARTITION)));
        assertThat(cursor.getOffset(), is(equalTo(OFFSET)));
    }
}
