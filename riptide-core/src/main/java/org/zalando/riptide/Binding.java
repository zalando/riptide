package org.zalando.riptide;

import javax.annotation.Nullable;

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

    public static <A> Binding<A> create(@Nullable final A attribute, final Route route) {
        return new Binding<>(attribute, route);
    }

}
