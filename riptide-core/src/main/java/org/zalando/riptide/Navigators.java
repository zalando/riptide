package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * Static factory method for built-in {@link Navigator navigators}.
 *
 * @see Navigator
 */
@API(status = STABLE)
public final class Navigators {

    private Navigators() {

    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status code series
     *
     * @return an HTTP status code series selector
     * @see Series
     */
    public static Navigator<Series> series() {
        return SeriesNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status.
     *
     * @return an HTTP status selector
     * @see HttpStatus
     * @see #statusCode()
     */
    public static Navigator<HttpStatus> status() {
        return StatusNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's status code.
     *
     * @return an HTTP status code selector
     * @see HttpStatus
     * @see #status()
     */
    public static Navigator<Integer> statusCode() {
        return StatusCodeNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects a binding based on the response's reason phrase.
     *
     * Be aware that this, even though it's standardized, could be changed by servers.
     *
     * @return an HTTP reason phrase selector
     * @see HttpStatus#getReasonPhrase()
     * @see #status()
     * @see #statusCode()
     */
    public static Navigator<String> reasonPhrase() {
        return ReasonPhraseNavigator.INSTANCE;
    }

    /**
     * A {@link Navigator} that selects the best binding based on the response's content type.
     *
     * @return a Content-Type selector
     * @see MediaType
     */
    public static Navigator<MediaType> contentType() {
        return ContentTypeNavigator.INSTANCE;
    }

}
