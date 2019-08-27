package org.zalando.riptide;

import org.springframework.http.client.*;

import java.io.*;

/**
 * @see Navigators#status()
 */
enum StatusCodeNavigator implements EqualityNavigator<Integer> {

    INSTANCE;

    @Override
    public Integer attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getRawStatusCode();
    }

}
