package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
public interface RequestArguments {

    // TODO top level?!
    interface Entity {
        void writeTo(HttpOutputMessage message) throws IOException;
    }

    URI getBaseUrl();

    UrlResolution getUrlResolution();

    HttpMethod getMethod();

    String getUriTemplate();

    List<Object> getUriVariables();

    URI getUri();

    <T> Optional<T> getAttribute(Attribute<T> attribute);

    Map<String, List<String>> getQueryParams();

    URI getRequestUri();

    Map<String, List<String>> getHeaders();

    Object getBody();

    Entity getEntity();

    RequestArguments withBaseUrl(@Nullable URI baseUrl);

    RequestArguments withUrlResolution(@Nullable UrlResolution resolution);

    RequestArguments withMethod(@Nullable HttpMethod method);

    RequestArguments withUriTemplate(@Nullable String uriTemplate);

    RequestArguments replaceUriVariables(List<Object> uriVariables);

    RequestArguments withUri(@Nullable URI uri);

    <T> RequestArguments withAttribute(Attribute<T> attribute, T value);

    RequestArguments withQueryParam(String name, String value);

    RequestArguments withQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withoutQueryParam(String name);

    RequestArguments replaceQueryParams(Map<String, ? extends Collection<String>> queryParams);

    RequestArguments withHeader(String name, String value);

    RequestArguments withHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withoutHeader(String name);

    RequestArguments replaceHeaders(Map<String, ? extends Collection<String>> headers);

    RequestArguments withBody(@Nullable Object body);

    RequestArguments withEntity(@Nullable Entity entity);

    static RequestArguments create() {
        return new DefaultRequestArguments();
    }

}
