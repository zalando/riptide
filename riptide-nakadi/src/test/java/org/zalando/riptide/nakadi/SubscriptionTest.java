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

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;

public class SubscriptionTest extends ObjectTheory {

    private static final String ID = "id";
    private static final String OWNER = "owner";
    private static final String GROUP = "group";
    private static final List<String> TYPES = Collections.emptyList();
    private static final String FROM = "begin";
    private static final String CREATED = "2016-12-24T18:59:30.123456";

    @DataPoints
    public static Subscription[] data = {
            null,
            new Subscription(null, null, null, null),
            new Subscription(ID, null, null, null, null, null),
            new Subscription(ID, null, null, null, null, null),
            new Subscription(OWNER, null, null, null),
            new Subscription(OWNER, null, null, null),
            new Subscription(null, GROUP, null, null),
            new Subscription(null, GROUP, null, null),
            new Subscription(null, null, TYPES, null),
            new Subscription(null, null, TYPES, null),
            new Subscription(null, null, null, FROM),
            new Subscription(null, null, null, FROM),
            new Subscription(null, null, null, null, null, CREATED),
            new Subscription(null, null, null, null, null, CREATED),
    };

    @Test
    public void getter() {
        Subscription sub = new Subscription(ID, OWNER, GROUP, TYPES, FROM, CREATED);
        assertThat(sub.getId(), is(equalTo(ID)));
        assertThat(sub.getOwningApplication(), is(equalTo(OWNER)));
        assertThat(sub.getConsumerGroup(), is(equalTo(GROUP)));
        assertThat(sub.getEventTypes(), is(equalTo(TYPES)));
        assertThat(sub.getReadFrom(), is(equalTo(FROM)));
        assertThat(sub.getCreatedAt(), is(equalTo(CREATED)));
    }
}
