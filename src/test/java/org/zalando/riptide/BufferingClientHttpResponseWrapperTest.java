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

import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BufferingClientHttpResponseWrapperTest {

    private final ClientHttpResponse response = mock(ClientHttpResponse.class);

    private BufferingClientHttpResponseWrapper unit;

    @Test
    public void buffersBody() throws IOException {
        final byte[] data = {0x13, 0x37};
        when(response.getBody()).thenReturn(new ByteArrayInputStream(data));

        unit = BufferingClientHttpResponseWrapper.buffer(response);

        assertThat(ByteStreams.toByteArray(unit.getBody()), is(data));
    }

    @Test
    public void skipsBodyIfNull() throws IOException {
        when(response.getBody()).thenReturn(null);

        unit = BufferingClientHttpResponseWrapper.buffer(response);

        assertThat(unit.getBody(), is(nullValue()));
    }

    @Test
    public void closesResponse() throws IOException {
        unit = BufferingClientHttpResponseWrapper.buffer(response);

        unit.close();

        verify(response).close();
    }

    @Test
    public void redirectsStatusFields() throws IOException {
        when(response.getStatusCode()).thenReturn(HttpStatus.ACCEPTED);
        when(response.getStatusText()).thenReturn("status-text");
        when(response.getRawStatusCode()).thenReturn(42);
        when(response.getHeaders()).thenReturn(new HttpHeaders());

        unit = BufferingClientHttpResponseWrapper.buffer(response);

        assertThat(unit.getStatusCode(), is(response.getStatusCode()));
        assertThat(unit.getStatusText(), is(response.getStatusText()));
        assertThat(unit.getRawStatusCode(), is(response.getRawStatusCode()));
        assertThat(unit.getHeaders(), is(response.getHeaders()));
    }
}
