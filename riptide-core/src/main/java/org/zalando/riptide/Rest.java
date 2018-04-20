package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.apiguardian.api.API.Status.DEPRECATED;

/**
 * @see Http
 * @see DefaultHttp
 */
@API(status = DEPRECATED, since = "2.5.0")
@Deprecated//(since = "2.5.0", forRemoval = true)
public final class Rest implements Http {

    private final Http http;

    Rest(final Http http) {
        this.http = http;
    }

    @Override
    public Requester get(final String uriTemplate, final Object... urlVariables) {
        return http.get(uriTemplate, urlVariables);
    }

    @Override
    public Requester get(final URI uri) {
        return http.get(uri);
    }

    @Override
    public Requester get() {
        return http.get();
    }

    @Override
    public Requester head(final String uriTemplate, final Object... urlVariables) {
        return http.head(uriTemplate, urlVariables);
    }

    @Override
    public Requester head(final URI uri) {
        return http.head(uri);
    }

    @Override
    public Requester head() {
        return http.head();
    }

    @Override
    public Requester post(final String uriTemplate, final Object... urlVariables) {
        return http.post(uriTemplate, urlVariables);
    }

    @Override
    public Requester post(final URI uri) {
        return http.post(uri);
    }

    @Override
    public Requester post() {
        return http.post();
    }

    @Override
    public Requester put(final String uriTemplate, final Object... urlVariables) {
        return http.put(uriTemplate, urlVariables);
    }

    @Override
    public Requester put(final URI uri) {
        return http.put(uri);
    }

    @Override
    public Requester put() {
        return http.put();
    }

    @Override
    public Requester patch(final String uriTemplate, final Object... urlVariables) {
        return http.patch(uriTemplate, urlVariables);
    }

    @Override
    public Requester patch(final URI uri) {
        return http.patch(uri);
    }

    @Override
    public Requester patch() {
        return http.patch();
    }

    @Override
    public Requester delete(final String uriTemplate, final Object... urlVariables) {
        return http.delete(uriTemplate, urlVariables);
    }

    @Override
    public Requester delete(final URI uri) {
        return http.delete(uri);
    }

    @Override
    public Requester delete() {
        return http.delete();
    }

    @Override
    public Requester options(final String uriTemplate, final Object... urlVariables) {
        return http.options(uriTemplate, urlVariables);
    }

    @Override
    public Requester options(final URI uri) {
        return http.options(uri);
    }

    @Override
    public Requester options() {
        return http.options();
    }

    @Override
    public Requester trace(final String uriTemplate, final Object... urlVariables) {
        return http.trace(uriTemplate, urlVariables);
    }

    @Override
    public Requester trace(final URI uri) {
        return http.trace(uri);
    }

    @Override
    public Requester trace() {
        return http.trace();
    }

    @Override
    public Requester execute(final HttpMethod method, final String uriTemplate, final Object... uriVariables) {
        return http.execute(method, uriTemplate, uriVariables);
    }

    @Override
    public Requester execute(final HttpMethod method, final URI uri) {
        return http.execute(method, uri);
    }

    @Override
    public Requester execute(final HttpMethod method) {
        return http.execute(method);
    }

    public static RestBuilder builder() {
        return new RestBuilder(Http.builder());
    }

}
