package org.zalando.riptide.stream;

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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class StreamFilterTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldFilterOnReadInteger() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream)) {
            final int filtered = 3;
            final int available = stream.available();
            int read = 0;
            while (unit.read() != -1) {
                read++;
            }
            assertThat(read, is(available - filtered));
        }
    }

    @Test
    public void shouldFilterOnReadBuffer() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream)) {
            final int filtered = 3;
            final int available = stream.available();
            int sum = 0, read = 0;
            final byte[] buffer = new byte[10];
            while ((read = unit.read(buffer, 0, buffer.length)) != -1) {
                sum += read;
            }
            assertThat(sum, is(available - filtered));
        }
    }

    @Test
    public void shouldFilterOnSkip() throws Exception {
        final InputStream stream = new ClassPathResource("account-sequence.json").getInputStream();

        try (final StreamFilter unit = new StreamFilter(stream, 5)) {
            final long filtered = 3;
            final long available = stream.available();
            long sum = 0, skipped = 0;
            while ((skipped = unit.skip(10)) != 0) {
                sum += skipped;
            }
            assertThat(sum, is(available - filtered));
        }
    }
}