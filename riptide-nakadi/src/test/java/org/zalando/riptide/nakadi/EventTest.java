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
import org.zalando.riptide.nakadi.Event.DataOperation;
import org.zalando.riptide.nakadi.Event.Metadata;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;

public class EventTest extends ObjectTheory {

    private static final JsonNodeFactory FACTORY = new JsonNodeFactory(true);
    private static final Metadata METADATA = new Event.Metadata(UUID.randomUUID(), null, null);
    private static final String DATATYPE = "b";
    private static final DataOperation DATAOP = Event.DataOperation.CREATE;
    private static final ObjectNode DATA = new ObjectNode(FACTORY).put("a", "a");

    public static Event randomEvent(final String type, final String value) {
        return new Event(new Event.Metadata(UUID.randomUUID(), OffsetDateTime.now(), UUID.randomUUID().toString()),
                type, Event.DataOperation.CREATE, new ObjectNode(FACTORY).put("key", value));
    }

    @DataPoints
    public static Event[] data = {
            null,
            new Event(null, null, null, null),
            new Event(METADATA, null, null, null),
            new Event(METADATA, null, null, null),
            new Event(null, DATATYPE, null, null),
            new Event(null, DATATYPE, null, null),
            new Event(null, null, DATAOP, null),
            new Event(null, null, DATAOP, null),
            new Event(null, null, null, DATA),
            new Event(null, null, null, DATA)
    };

    @Test
    public void getter() {
        Event event = new Event(METADATA, DATATYPE, DATAOP, DATA);
        assertThat(event.getMetadata(), is(equalTo(METADATA)));
        assertThat(event.getDataType(), is(equalTo(DATATYPE)));
        assertThat(event.getDataOp(), is(equalTo(DATAOP)));
        assertThat(event.getDataOp().getValue(), is(equalTo(DATAOP.getValue())));
        assertThat(event.getData(), is(equalTo(DATA)));
    }

    @Test
    @Hack
    @OhNoYouDidnt
    public void coverage() {
        Event.DataOperation.valueOf(Event.DataOperation.CREATE.toString());
    }
}
