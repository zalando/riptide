package org.zalando.riptide;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * @see Navigators#status()
 */
enum StatusNavigator implements EqualityNavigator<HttpStatus> {

    INSTANCE;

    @Override
    public HttpStatus attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getStatusCode();
    }

}
