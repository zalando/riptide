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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.conn.ConnectionReleaseTrigger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import lombok.SneakyThrows;

class RestAsyncClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse response;

    RestAsyncClientHttpResponse(final ClientHttpResponse response) {
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
    public InputStream getBody() throws IOException {
        return new FilterInputStream(response.getBody()) {
            @Override
            public void close() throws IOException {
                if (in instanceof ConnectionReleaseTrigger) {
                    ((ConnectionReleaseTrigger) in).abortConnection();
                }
                super.close();
            }
        };
    }

    @Override
    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }

    @Override
    @SneakyThrows
    public void close() {
        response.close();
    }

}
