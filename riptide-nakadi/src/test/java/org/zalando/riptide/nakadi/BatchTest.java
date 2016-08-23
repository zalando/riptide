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
import static org.zalando.riptide.nakadi.EventTest.randomEvent;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;

public class BatchTest extends ObjectTheory {

    private static final String PARTITION = "0";
    private static final String OFFSET = "1";
    private static final Cursor CURSOR = new Cursor(PARTITION, OFFSET);
    private static final List<Event> EVENTS =
            Arrays.asList(randomEvent("type-0", "value-0"), randomEvent("type-1", "value-1"));

    @DataPoints
    public static Object[] data = {
            null,
            new Batch(null, null),
            new Batch(CURSOR, null),
            new Batch(CURSOR, null),
            new Batch(null, EVENTS),
            new Batch(null, EVENTS),
    };

    @Test
    public void getter() {
        Batch batch = new Batch(CURSOR, EVENTS);
        batch.equals(null);
        assertThat(batch.getCursor(), is(equalTo(CURSOR)));
        assertThat(batch.getEvents(), is(equalTo(EVENTS)));
    }
}
