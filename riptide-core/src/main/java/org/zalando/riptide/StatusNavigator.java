package org.zalando.riptide;

import org.springframework.http.*;
import org.springframework.http.client.*;

import java.io.*;

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
