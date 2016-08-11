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
    private static final List<String> TYPES = Collections.emptyList();

    @DataPoints
    public static Subscription[] data = {
            null,
            new Subscription(null, null, null),
            new Subscription(ID, null, null),
            new Subscription(ID, null, null),
            new Subscription(null, OWNER, null),
            new Subscription(null, OWNER, null),
            new Subscription(null, null, TYPES),
            new Subscription(null, null, TYPES)
    };

    @Test
    public void getter() {
        Subscription sub = new Subscription(ID, OWNER, TYPES);
        assertThat(sub.getId(), is(equalTo(ID)));
        assertThat(sub.getOwningApplication(), is(equalTo(OWNER)));
        assertThat(sub.getEventTypes(), is(equalTo(TYPES)));
    }
}
