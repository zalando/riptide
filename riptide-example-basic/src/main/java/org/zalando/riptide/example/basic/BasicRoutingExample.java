package org.zalando.riptide.example.basic;

import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.Http;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.http.HttpStatus.Series.REDIRECTION;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;

/**
 * Demonstrates Riptide's core response-routing API:
 * <ul>
 *   <li>Routing on HTTP status series ({@code SUCCESSFUL}, {@code REDIRECTION})</li>
 *   <li>Extracting a {@code Location} header from a redirect response</li>
 *   <li>Deserializing a typed JSON body from a successful response</li>
 * </ul>
 *
 * <p>See {@code BasicRoutingExampleTest} for a runnable version wired through the
 * Spring Boot starter path.
 */
public class BasicRoutingExample {

    private final Http http;

    public BasicRoutingExample(Http http) {
        this.http = http;
    }

    /**
     * Issues a GET request and captures the {@code Location} header when the server
     * responds with a redirect (3xx).
     *
     * <p><strong>Note:</strong> only one route is registered ({@code REDIRECTION}). Any other
     * response series (e.g. a {@code 200 OK} or a {@code 5xx}) will cause Riptide to throw an
     * {@link org.zalando.riptide.UnexpectedResponseException} via the returned
     * {@link java.util.concurrent.CompletableFuture}. In production, register an
     * {@code anySeries()} fallback route or use
     * {@link org.zalando.riptide.problem.ProblemRoute#problemHandling()} to avoid silent failures.
     */
    public URI followRedirect(String path) throws Exception {
        AtomicReference<URI> location = new AtomicReference<>();

        http.get(path)
                .dispatch(series(),
                        on(REDIRECTION).call((ClientHttpResponse response) ->
                                location.set(response.getHeaders().getLocation())))
                .join();

        return location.get();
    }

    /**
     * Issues a GET request and deserializes the JSON response body into an instance of
     * {@code type} when the server responds with a success (2xx).
     *
     * <p><strong>Note:</strong> only one route is registered ({@code SUCCESSFUL}). Any other
     * response series will cause Riptide to throw an
     * {@link org.zalando.riptide.UnexpectedResponseException} via the returned
     * {@link java.util.concurrent.CompletableFuture}. In production, register an
     * {@code anySeries()} fallback route or use
     * {@link org.zalando.riptide.problem.ProblemRoute#problemHandling()} to avoid silent failures.
     */
    public <T> T fetchBody(String path, Class<T> type) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();

        http.get(path)
                .dispatch(series(),
                        on(SUCCESSFUL).call(type, result::set))
                .join();

        return result.get();
    }

    /**
     * Returns the underlying {@link Http} client, useful for extending this example
     * with additional routing patterns.
     */
    public Http getHttp() {
        return http;
    }
}
