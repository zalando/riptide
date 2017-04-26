package org.zalando.riptide;

import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.net.URI;

// TODO introduce UrlResolutionStrategy interface in the future, if needed
public enum UrlResolution {

    /**
     *
     */
    RFC {
        @Override
        URI resolve(final URI baseUrl, final URI uri) {
            return baseUrl.resolve(uri);
        }
    },

    /**
     *
     */
    LAX {
        @Override
        URI resolve(final URI baseUrl, final URI uri) {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUri(baseUrl);

            builder.scheme(either(uri.getScheme(), baseUrl.getScheme()));
            builder.userInfo(either(uri.getRawUserInfo(), baseUrl.getRawUserInfo()));
            builder.host(either(uri.getHost(), baseUrl.getHost()));
            builder.port(either(uri.getPort(), baseUrl.getPort()));
            builder.fragment(either(uri.getRawFragment(), baseUrl.getRawFragment()));

            if (uri.isAbsolute()) {
                builder.replacePath(uri.getRawPath());
            } else {
                builder.path(uri.getRawPath());
            }

            if (uri.getQuery() != null) {
                builder.query(uri.getRawQuery());
            }

            return builder.build().toUri();
        }

        @Nullable
        private <T> T either(@Nullable final T left, @Nullable final T right) {
            return left == null ? right : left;
        }
    };

    abstract URI resolve(final URI baseUrl, final URI uri);

}
