package org.zalando.riptide.httpclient;

/*
 * ⁣​
 * Riptide: HTTP Client
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.conn.EofSensorInputStream;
import org.junit.Test;
import org.springframework.http.client.ClientHttpResponse;

public class RestAsyncClientHttpResponseBodyTest {

    @Test
    public void shouldCallCloseOnNormalStreams() throws IOException {
        InputStream stream = mock(InputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        try (RestAsyncClientHttpResponse unit = new RestAsyncClientHttpResponse(response)) {
            unit.getBody().close();
        }

        verify(stream, times(1)).close();
    }

    @Test
    public void shouldCallCloseAndAbortOnConnectionReleaseTrigger() throws IOException {
        EofSensorInputStream stream = mock(EofSensorInputStream.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(stream);

        try (RestAsyncClientHttpResponse unit = new RestAsyncClientHttpResponse(response)) {
            unit.getBody().close();
        }

        verify(stream, times(1)).abortConnection();
    }
}
