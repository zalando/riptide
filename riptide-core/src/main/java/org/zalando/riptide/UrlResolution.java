package org.zalando.riptide;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// TODO introduce UrlResolutionStrategy interface in the future, if needed
public enum UrlResolution {

    /**
     * TODO document
     */
    RFC {
        @Override
        URI resolve(final URI baseUrl, final URI uri) {
            return baseUrl.resolve(uri);
        }
    },

    /**
     * TODO document
     */
    APPEND {
        @Override
        URI resolve(final URI baseUrl, final URI uri) {
            return UriComponentsBuilder.fromUri(baseUrl)
                    .pathSegment(uri.getRawPath())
                    .query(uri.getRawQuery())
                    .build()
                    .toUri();
        }
    };

    /**
     *
     * @param baseUrl absolute, non-empty base URL
     * @param uri non-absolute
     * @return the resolved, absolute request URI
     */
    abstract URI resolve(final URI baseUrl, final URI uri);

}
