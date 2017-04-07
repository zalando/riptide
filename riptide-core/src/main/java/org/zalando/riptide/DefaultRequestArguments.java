package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import java.net.URI;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class DefaultRequestArguments implements RequestArguments {

    @Wither
    HttpMethod method;

    @Wither
    URI baseUrl;

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

    public DefaultRequestArguments() {
        this(null, null, null, ImmutableList.of(), null, ImmutableMultimap.of(), null, ImmutableMultimap.of(), null);
    }

    public DefaultRequestArguments withRequestUri() {
        // expand uri template
        final URI uri = Optional.ofNullable(getUri())
                .orElseGet(() -> fromUriString(getUriTemplate())
                        .buildAndExpand(getUriVariables().toArray())
                        .encode()
                        .toUri());

        // resolve uri against base url
        final URI resolved = getBaseUrl() == null ? uri : getBaseUrl().resolve(uri);

        // encode query params
        final MultiValueMap<String, String> queryParams;
        {
            final UriComponentsBuilder components = UriComponentsBuilder.newInstance();
            getQueryParams().entries().forEach(entry
                    -> components.queryParam(entry.getKey(), entry.getValue()));
            queryParams = components.build().encode().getQueryParams();
        }

        // build request uri
        final URI requestUri = fromUri(resolved)
                .queryParams(queryParams)
                .build(true).normalize().toUri();

        return withRequestUri(requestUri);
    }

}
