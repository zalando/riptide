package de.zalando;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

final class StatusCodeSelector implements Selector<HttpStatus> {

    @Override
    public HttpStatus attributeOf(ClientHttpResponse response) {
        try {
            return response.getStatusCode();
        } catch (IOException e) {
            // TODO is this the correct exception type?
            throw new IllegalStateException(e);
        }
    }

}
