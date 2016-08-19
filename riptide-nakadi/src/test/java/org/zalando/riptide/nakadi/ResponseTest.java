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

public class ResponseTest extends ObjectTheory {

    private static final String EID = "eid";
    private static final String STATUS = "submitted";
    private static final String STEP = "none";
    private static final String DETAIL = "detail";

    @DataPoints
    public static Response[] data = {
            null,
            new Response(null, null, null, null),
            new Response(EID, null, null, null),
            new Response(EID, null, null, null),
            new Response(null, STATUS, null, null),
            new Response(null, STATUS, null, null),
            new Response(null, null, STEP, null),
            new Response(null, null, STEP, null),
            new Response(null, null, null, DETAIL),
            new Response(null, null, null, DETAIL)
    };

    @Test
    public void getter() {
        Response resp = new Response(EID, STATUS, STEP, DETAIL);
        assertThat(resp.getEid(), is(equalTo(EID)));
        assertThat(resp.getPublishingStatus(), is(equalTo(STATUS)));
        assertThat(resp.getStep(), is(equalTo(STEP)));
        assertThat(resp.getDetail(), is(equalTo(DETAIL)));
    }
}
