package org.zalando.riptide.stream;

import static org.hamcrest.Matchers.instanceOf;

/*
 * ⁣​
 * Riptide: Stream
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParser;

public final class StreamSpliteratorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldNotSupportParallelExecution() {
        JsonParser parser = mock(JsonParser.class);
        try (StreamSpliterator<Object> spliterator = new StreamSpliterator<>(null, parser, null)) {
            assertNull(spliterator.trySplit());
        }
    }

    @Test
    public void shouldNotPredictEstimateSize() {
        JsonParser parser = mock(JsonParser.class);
        try (StreamSpliterator<?> spliterator = new StreamSpliterator<>(null, parser, null)) {
            assertThat(spliterator.estimateSize(), is(Long.MAX_VALUE));
        }
    }

    @Test
    public void shouldWrapIOException() throws Exception {
        exception.expect(UncheckedIOException.class);
        exception.expectCause(instanceOf(IOException.class));

        JsonParser parser = mock(JsonParser.class);
        Mockito.doThrow(new IOException()).when(parser).close();
        try (StreamSpliterator<?> spliterator = new StreamSpliterator<>(null, parser, null)) {
            // nothing to do.
        }
    }
}
