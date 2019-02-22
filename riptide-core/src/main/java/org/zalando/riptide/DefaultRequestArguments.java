package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.apiguardian.api.API;
import org.springframework.http.HttpMethod;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;

import static org.apiguardian.api.API.Status.INTERNAL;

@API(status = INTERNAL)
@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
final class DefaultRequestArguments implements RequestArguments {

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
    ImmutableMap<Attribute<?>, Object> attributes;

    @Wither
    ImmutableMultimap<String, String> queryParams;

    @Wither
    URI requestUri;

    @Wither
    ImmutableMultimap<String, String> headers;

    @Wither
    Object body;

    @Override
    public <T> Optional<T> getAttribute(final Attribute<T> attribute) {
        @SuppressWarnings("unchecked")
        @Nullable final T value = (T) attributes.get(attribute);
        return Optional.ofNullable(value);
    }

}
