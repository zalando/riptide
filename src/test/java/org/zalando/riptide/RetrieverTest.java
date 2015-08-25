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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.zalando.riptide.Capture.captured;

public final class RetrieverTest {

    @Test
    public void shouldNotRetrieveNullOnCaptured() {
        final Capture<Void> value = captured(null);

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(String.class), is(false));
        assertThat(unit.retrieve(String.class), is(empty()));
    }

    @Test
    public void shouldRetrieveNullOnTypedCaptured() {
        final Capture<String> value = captured(null, TypeToken.of(String.class));

        final Retriever unit = new Retriever(value);

        // TODO is that expected? hasRetrieved=true, retrieve(..).isPresent()=false
        assertThat(unit.hasRetrieved(String.class), is(true));
        assertThat(unit.retrieve(String.class), is(empty()));
    }

    @Test
    public void shouldRetrieveCaptured() {
        final Capture<String> value = captured("");

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(String.class), is(true));
        assertThat(unit.retrieve(String.class), is(not(empty())));
    }

    @Test
    public void shouldNotRetrieveCapturedOnParameterizedType() {
        final TypeToken<List<String>> type = new TypeToken<List<String>>() {};
        final Capture<List<String>> value = captured(newArrayList());

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(type), is(false));
        assertThat(unit.retrieve(type), is(empty()));
    }

    @Test
    public void shouldRetrieveTypedCaptured() {
        final TypeToken<List<String>> type = new TypeToken<List<String>>() {};
        final Capture<List<String>> value = captured(newArrayList(), type);

        final Retriever unit = new Retriever(value);

        assertThat(unit.hasRetrieved(type), is(true));
        assertThat(unit.retrieve(type), is(not(empty())));
    }

}
