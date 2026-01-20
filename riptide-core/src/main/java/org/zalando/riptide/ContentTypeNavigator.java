package org.zalando.riptide;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

/**
 * @see Navigators#contentType()
 */
enum ContentTypeNavigator implements EqualityNavigator<MediaType> {

    INSTANCE;


    static final Comparator<MediaType> SPECIFIC_COMPARATOR = (t1, t2) -> {
        if (t1.equals(t2)) return 0;
        return t2.isMoreSpecific(t1) ? 1 : -1;
    };

    @Nullable
    @Override
    public MediaType attributeOf(final ClientHttpResponse response) {
        return response.getHeaders().getContentType();
    }

    @Override
    public Optional<Route> navigate(@Nullable final MediaType contentType, final RoutingTree<MediaType> tree) throws IOException {
        final Optional<Route> exact = exactMatch(contentType, tree);

        if (exact.isPresent()) {
            return exact;
        }

        return bestMatch(contentType, tree);

    }

    private Optional<Route> exactMatch(@Nullable final MediaType contentType,
            final RoutingTree<MediaType> tree) throws IOException {

        return EqualityNavigator.super.navigate(contentType, tree);
    }

    private Optional<Route> bestMatch(@Nullable final MediaType contentType,
            final RoutingTree<MediaType> tree) {

        return tree.keySet().stream()
                .filter(mediaType -> mediaType.includes(contentType))
                .min(SPECIFIC_COMPARATOR)
                .flatMap(tree::get);
    }
}
