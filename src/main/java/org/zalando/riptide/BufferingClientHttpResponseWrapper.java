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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

    static BufferingClientHttpResponseWrapper buffer(ClientHttpResponse response) throws IOException {
        final BufferingClientHttpResponseWrapper wrapper = new BufferingClientHttpResponseWrapper(response);
        wrapper.buffer();
        return wrapper;
    }

    private final ClientHttpResponse response;

    private byte[] body;

    private BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
        this.response = response;
    }


    @Override
    public HttpStatus getStatusCode() throws IOException {
        return response.getStatusCode();
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return response.getRawStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return response.getStatusText();
    }

    @Override
    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }

    @Override
    public InputStream getBody() throws IOException {
        return body == null ? null : new ByteArrayInputStream(body);
    }

    @Override
    public void close() {
        response.close();
    }

    private void buffer() throws IOException {
        if (response.getBody() != null) {
            body = StreamUtils.copyToByteArray(response.getBody());
        }
    }

}
