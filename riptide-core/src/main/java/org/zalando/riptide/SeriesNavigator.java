package org.zalando.riptide;

import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

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
