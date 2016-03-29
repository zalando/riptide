package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
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
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockAsyncClientHttpRequest;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;

public final class AsyncClientHttpRequestAdapterTest {

    private final MockAsyncClientHttpRequest original = new MockAsyncClientHttpRequest(GET, URI.create("/"));
    private final ClientHttpRequest unit = new AsyncClientHttpRequestAdapter(original);

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotSupportExecute() throws IOException {
        unit.execute();
    }
    
    @Test
    public void shouldRetrieveBody() throws IOException {
        assertThat(unit.getBody(), is(original.getBody()));
    }
    
    @Test
    public void shouldRetrieveMethod() {
        assertThat(unit.getMethod(), is(GET));
    }

    @Test
    public void shouldRetrieveURI() {
        assertThat(unit.getURI(), hasToString("/"));
    }
    
    @Test
    public void shouldRetrieveHeaders() {
        assertThat(unit.getHeaders().isEmpty(), is(true));
    }
    
}