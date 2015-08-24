package org.zalando.riptide;

public final class Actions {

    public static <X extends Exception> EntityConsumer<X, X> propagate() {
        return entity -> {
            throw entity;
        };
    }

}
