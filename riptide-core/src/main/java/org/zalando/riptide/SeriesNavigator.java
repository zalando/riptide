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

    @Override
    public String toString(final Series attribute) {
        return attribute.value() + "xx";
    }

    @Override
    public String toString() {
        return "Series";
    }

}
