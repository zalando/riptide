package org.zalando.riptide;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// TODO introduce UrlResolutionStrategy interface in the future, if needed
public enum UrlResolution {

    /**
     * Resolves a given relativce URI by following the rules defined in RFC 3986.
     *
     * @see <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
     * @see URI#resolve(URI)
     */
    RFC {
        @Override
        URI resolve(final URI baseUrl, final URI uri) {
            return baseUrl.resolve(uri);
        }
    },

    /**
     * Resolves a given relative URI by appending its path to and merging its query parameters with the given base URL.
     *
     * @see UriComponentsBuilder#pathSegment(String...)
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
