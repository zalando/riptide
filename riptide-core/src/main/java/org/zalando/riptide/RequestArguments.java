package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.springframework.http.HttpMethod;
import java.net.URI;

public interface RequestArguments {

    URI getBaseUrl();

    HttpMethod getMethod();

    String getUriTemplate();

    ImmutableList<Object> getUriVariables();

    URI getUri();

    ImmutableMultimap<String, String> getQueryParams();

    URI getRequestUri();

    ImmutableMultimap<String, String> getHeaders();

    Object getBody();

}
