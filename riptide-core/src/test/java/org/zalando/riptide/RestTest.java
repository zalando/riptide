package org.zalando.riptide;

/*
 * ⁣​
 * Riptide: Core
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

import org.junit.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public final class RestTest {

    @Test
    public void shouldCreateAndClose() throws IOException {
        try (Rest rest = Rest.create("https://www.example.com")) {
            assertThat(rest, is(notNullValue()));
        }
    }

    @Test
    public void shouldCloseEvenIfThereIsNothingToClose() throws IOException {
        Rest.create(new SimpleClientHttpRequestFactory(), emptyList()).close();
    }

}