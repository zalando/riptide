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

import com.google.common.reflect.TypeToken;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RetrieverTest {

    @Test
    public void shouldNotRetrieveNullOnCaptured() {
        final Captured value = new Captured(null);

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(String.class), is(false));
        assertThat(unit.retrieve(String.class), is(Optional.empty()));
    }

    @Test
    public void shouldRetrieveNullOnTypedCaptured() {
        final Captured value = new TypedCaptured(null, TypeToken.of(String.class));

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(String.class), is(true));
        assertThat(unit.retrieve(String.class), is(Optional.empty()));
    }

    @Test
    public void shouldRetrieveCaptured() {
        final Captured value = new Captured("");

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(String.class), is(true));
        assertThat(unit.retrieve(String.class), is(not(Optional.empty())));
    }

    @Test
    public void shouldNotRetrieveCapturedOnParameterizedType() {
        final TypeToken<List<String>> type = new TypeToken<List<String>>() {};
        final Captured value = new Captured(newArrayList());

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(type), is(false));
        assertThat(unit.retrieve(type), is(Optional.empty()));
    }

    @Test
    public void shouldRetrieveTypedCaptured() {
        final TypeToken<List<String>> type = new TypeToken<List<String>>() {};
        final Captured value = new TypedCaptured(newArrayList(), type);

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(type), is(true));
        assertThat(unit.retrieve(type), is(not(Optional.empty())));
    }

}
