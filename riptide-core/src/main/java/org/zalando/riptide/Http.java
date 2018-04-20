package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * Central class for actual asynchronous HTTP-based communication. Http is loosely modeled after the HTTP protocol,
 * i.e. you start with a method and by a URL and optionally followed query parameters, headers and a body:
 *
 * <pre>{@code http.get("/users")
 *     .queryParam("active", "true")
 *     .accept(APPLICATION_JSON)
 *     .dispatch(..)}</pre>
 *
 * @see RestTemplate
 * @see AsyncRestTemplate
 */
@API(status = STABLE)
public interface Http {

    Requester get(String uriTemplate, Object... urlVariables);
    Requester get(URI uri);
    Requester get();

    Requester head(String uriTemplate, Object... urlVariables);
    Requester head(URI uri);
    Requester head();

    Requester post(String uriTemplate, Object... urlVariables);
    Requester post(URI uri);
    Requester post();

    Requester put(String uriTemplate, Object... urlVariables);
    Requester put(URI uri);
    Requester put();

    Requester patch(String uriTemplate, Object... urlVariables);
    Requester patch(URI uri);
    Requester patch();

    Requester delete(String uriTemplate, Object... urlVariables);
    Requester delete(URI uri);
    Requester delete();

    Requester options(String uriTemplate, Object... urlVariables);
    Requester options(URI uri);
    Requester options();

    Requester trace(String uriTemplate, Object... urlVariables);
    Requester trace(URI uri);
    Requester trace();

    Requester execute(HttpMethod method, String uriTemplate, Object... uriVariables);
    Requester execute(HttpMethod method, URI uri);
    Requester execute(HttpMethod method);

    static HttpBuilder builder() {
        return new DefaultHttpBuilder();
    }

}
