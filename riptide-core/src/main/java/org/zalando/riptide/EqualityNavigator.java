package org.zalando.riptide;

import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

import static org.apiguardian.api.API.Status.STABLE;

@API(status = STABLE)
@FunctionalInterface
public interface EqualityNavigator<A> extends Navigator<A> {

    /**
     * Retrieves an attribute from the given response
     *
     * @param response the incoming response
     * @return an attribute based on the response which is then used to select the correct binding
     * @throws IOException if accessing the response failed
     */
    @Nullable
    A attributeOf(ClientHttpResponse response) throws IOException;

    @Override
    default Optional<Route> navigate(final ClientHttpResponse response, final RoutingTree<A> tree) throws IOException {
        @Nullable final A attribute = attributeOf(response);
        return navigate(attribute, tree);
    }

    default Optional<Route> navigate(@Nullable final A attribute, final RoutingTree<A> tree) throws IOException {
        return attribute == null ? tree.getWildcard() : tree.get(attribute);
    }

}
