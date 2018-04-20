package org.zalando.riptide;

import org.apiguardian.api.API;

import javax.annotation.Nullable;

import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.STABLE;

/**
 * Bindings are the building blocks of {@link RoutingTree routing trees}. They bind an {@link #getAttribute() attribute}
 * to a {@link #getRoute() route}. A binding represents a choice to the {@link Navigator navigator} which route to
 * follow.
 *
 * @param <A> generic attribute type
 */

@API(status = STABLE)
public final class Binding<A> {

    private final A attribute;
    private final Route route;

    private Binding(@Nullable final A attribute, final Route route) {
        this.attribute = attribute;
        this.route = route;
    }

    @Nullable
    public A getAttribute() {
        return attribute;
    }

    public Route getRoute() {
        return route;
    }

    /**
     * A static factory to create bindings. {@link Bindings#on(Object)} and {@link PartialBinding} provide the same
     * functionality but with some more syntactic sugar.
     *
     * @param attribute attribute to bind
     * @param route route to bind to
     * @param <A> generic attribute type
     * @return a binding of attribute to route
     * @see PartialBinding
     */
    // TODO package private?
    @API(status = INTERNAL)
    public static <A> Binding<A> create(@Nullable final A attribute, final Route route) {
        return new Binding<>(attribute, route);
    }

}
