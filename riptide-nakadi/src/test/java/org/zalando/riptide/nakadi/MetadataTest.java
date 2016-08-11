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

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;

public class MetadataTest extends ObjectTheory {

    private static final UUID RUUID = UUID.randomUUID();
    private static final OffsetDateTime TIME = OffsetDateTime.now();

    @DataPoints
    public static Event.Metadata[] data = {
            null,
            new Event.Metadata(null, null, null),
            new Event.Metadata(RUUID, null, null),
            new Event.Metadata(RUUID, null, null),
            new Event.Metadata(null, TIME, null),
            new Event.Metadata(null, TIME, null),
            new Event.Metadata(null, null, RUUID.toString()),
            new Event.Metadata(null, null, RUUID.toString())
    };

    @Test
    public void getter() {
        Event.Metadata data = new Event.Metadata(RUUID, TIME, RUUID.toString());
        assertThat(data.getEid(), is(equalTo(RUUID)));
        assertThat(data.getOccurredAt(), is(equalTo(TIME)));
        assertThat(data.getFlowId(), is(equalTo(RUUID.toString())));
    }
}
