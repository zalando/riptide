package org.zalando.riptide;

import org.springframework.http.HttpStatus.*;
import org.springframework.http.client.*;

import java.io.*;

/**
 * @see Navigators#series()
 */
enum SeriesNavigator implements EqualityNavigator<Series> {

    INSTANCE;

    @Override
    public Series attributeOf(final ClientHttpResponse response) throws IOException {
        return response.getStatusCode().series();
    }

}
