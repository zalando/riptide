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

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.zalando.fauxpas.TryWith.tryWith;

@SuppressWarnings("serial")
public final class NoRouteException extends RestClientException {

    private static final int MAX_BODY_BYTES_TO_READ = 8192;

    private final ClientHttpResponse response;

    public NoRouteException(final ClientHttpResponse response) throws IOException {
        super(formatMessage(response));
        this.response = response;
    }

    private static String formatMessage(final ClientHttpResponse response) throws IOException {
        return String.format("Unable to dispatch response: %d - %s\n%s\n%s",
                response.getRawStatusCode(), response.getStatusText(), response.getHeaders(),
                readStartOfBody(response));
    }

    private static String readStartOfBody(final ClientHttpResponse response) throws IOException {
        return tryWith(response.getBody(), stream -> {
            if (stream == null) {
                return "";
            }

            final byte[] buffer = new byte[MAX_BODY_BYTES_TO_READ];
            final int read = stream.read(buffer);
            final Charset charset = extractCharset(response);
            return new String(buffer, 0, read, charset);
        });
    }

    private static Charset extractCharset(final ClientHttpResponse response) {
        return Optional.ofNullable(response.getHeaders().getContentType())
                .map(MediaType::getCharSet)
                .orElse(ISO_8859_1);
    }

    public ClientHttpResponse getResponse() {
        return response;
    }

}
