package org.zalando.riptide.httpclient;

import org.apache.http.client.methods.HttpUriRequest;
import org.springframework.http.HttpHeaders;

import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.TRANSFER_ENCODING;

final class Headers {

    private Headers() {

    }

    static void writeHeaders(final HttpHeaders headers, final HttpUriRequest request) {
        headers.forEach((name, values) ->
                values.forEach(value ->
                        request.addHeader(name, value)));

        request.removeHeaders(CONTENT_LENGTH);
        request.removeHeaders(TRANSFER_ENCODING);
    }

}
