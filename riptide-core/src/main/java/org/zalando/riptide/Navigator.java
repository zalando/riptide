package org.zalando.riptide;

import com.google.common.reflect.TypeToken;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;

import static org.apiguardian.api.API.Status.STABLE;

/**
 * A Navigator chooses among the {@link Binding bindings} of a {@link RoutingTree routing tree}. The act of traversing
 * a routing tree by choosing a binding and following its {@link Binding#getRoute() associated route} is called
 * nested dispatch.
 *
 * @param <A> generic attribute type
 */
@API(status = STABLE)
@FunctionalInterface
public interface Navigator<A> {

    default TypeToken<A> getType() {
        return new TypeToken<A>(getClass()) {};
    }

    Optional<Route> navigate(final ClientHttpResponse response, final RoutingTree<A> tree) throws IOException;

}
