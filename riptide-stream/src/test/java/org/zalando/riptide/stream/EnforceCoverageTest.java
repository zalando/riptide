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

import com.google.gag.annotation.remark.Hack;
import com.google.gag.annotation.remark.OhNoYouDidnt;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

@Hack
@OhNoYouDidnt
public final class EnforceCoverageTest {

    @Test
    public void shouldUseStreamsConstructor() {
        new Streams();
    }

    @Test
    public void shouldUseStreamConverterConstructor() {
        Streams.streamConverter();
    }

    @Test
    public void shouldUseStreamSpliteratorTrySplit() {
        assertNull(new StreamSpliterator<>(null, null, null).trySplit());
    }
    @Test
    public void shouldUseStreamSpliteratorEstimateSize() {
        assertThat(new StreamSpliterator<>(null, null, null).estimateSize(), is(Long.MAX_VALUE));
    }
}
