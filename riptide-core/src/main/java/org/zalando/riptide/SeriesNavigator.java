package org.zalando.riptide;

import org.springframework.http.HttpStatus;
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
        return HttpStatus.resolve(response.getStatusCode().value()).series();
    }

}
