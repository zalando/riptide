package org.zalando.riptide;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

import static org.springframework.http.MediaType.SPECIFICITY_COMPARATOR;

/**
 * @see Navigators#contentType()
 */
enum ContentTypeNavigator implements EqualityNavigator<MediaType> {

    INSTANCE;


    @Nullable
    @Override
    public MediaType attributeOf(final ClientHttpResponse response) throws IOException {
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
                .sorted(SPECIFICITY_COMPARATOR)
                .findFirst()
                .flatMap(tree::get);
    }
}
