package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;

import java.net.URI;

import static org.apiguardian.api.API.Status.INTERNAL;

// TODO package private?
@API(status = INTERNAL)
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class DefaultRequestArguments implements RequestArguments {

    @Wither
    HttpMethod method;

    @Wither
    URI baseUrl;

    @Wither
    UrlResolution urlResolution;

    @Wither
    String uriTemplate;

    @Wither
    @Singular
    ImmutableList<Object> uriVariables;

    @Wither
    URI uri;

    @Wither
    ImmutableMultimap<String, String> queryParams;

    @Wither
    URI requestUri;

    @Wither
    ImmutableMultimap<String, String> headers;

    @Wither
    Object body;

}
