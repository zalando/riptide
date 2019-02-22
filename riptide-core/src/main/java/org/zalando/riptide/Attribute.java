package org.zalando.riptide;

public interface Attribute<T> {

    static <T> Attribute<T> generate() {
        return new DefaultAttribute<>();
    }

}
