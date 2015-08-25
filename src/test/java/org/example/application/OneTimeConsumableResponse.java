package org.example.application;

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
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

class OneTimeConsumableResponse implements ClientHttpResponse {

    private final String bodyContent;

    OneTimeConsumableResponse(final String bodyContent) {
        this.bodyContent = bodyContent;
    }

    @Override
    public HttpHeaders getHeaders() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    @Override
    public HttpStatus getStatusCode() throws IOException {
        return HttpStatus.OK;
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return getStatusCode().value();
    }

    @Override
    public String getStatusText() throws IOException {
        return getStatusCode().getReasonPhrase();
    }

    @Override
    public void close() {
        try {
            getBody().close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public InputStream getBody() throws IOException {
        return new InputStream() {
            private boolean closed = false;

            private final ByteArrayInputStream bytes = new ByteArrayInputStream(bodyContent.getBytes());

            @Override
            public int read() throws IOException {
                if (closed) {
                    throw new IOException("Already closed");
                }
                return bytes.read();
            }

            @Override
            public void close() throws IOException {
                if (closed) {
                    throw new IOException("Already closed");
                }
                closed = true;
            }
        };
    }
}
