package org.zalando.riptide;

import org.springframework.http.HttpStatus.Series;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Objects;

/**
 * @see Navigators#series()
 */
enum SeriesNavigator implements EqualityNavigator<Series> {

    INSTANCE;

    @Override
    public Series attributeOf(final ClientHttpResponse response) throws IOException {
        final int statusCode = response.getStatusCode().value();
        return Objects.requireNonNullElse(Series.resolve(statusCode), Series.SERVER_ERROR);
    }

}
