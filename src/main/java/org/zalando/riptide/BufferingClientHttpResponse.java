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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static java.util.Optional.empty;

final class BufferingClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse response;
    private final Optional<byte[]> body;

    private BufferingClientHttpResponse(final ClientHttpResponse response, final Optional<byte[]> body) {
        this.response = response;
        this.body = body;
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
    public InputStream getBody() {
        return body.map(ByteArrayInputStream::new).orElse(null);
    }

    @Override
    public void close() {
        response.close();
    }

    public static BufferingClientHttpResponse buffer(final ClientHttpResponse response) throws IOException {
        if (response.getBody() == null) {
            return new BufferingClientHttpResponse(response, empty());
        } else {
            final byte[] bytes = ByteStreams.toByteArray(response.getBody());
            return new BufferingClientHttpResponse(response, Optional.of(bytes));
        }
    }

}
