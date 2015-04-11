package de.zalando;

import java.lang.reflect.Type;
import java.util.function.Function;

public final class Binding<A, I, O> implements Function<I, O> {

    private final A attribute;
    private final Type type;
    private final Function<I, O> mapper;

    Binding(A attribute, Type type, Function<I, O> mapper) {
        this.attribute = attribute;
        this.type = type;
        this.mapper = mapper;
    }

    public A getAttribute() {
        return attribute;
    }

    public Type getType() {
        return type;
    }

    @Override
    public O apply(I i) {
        return mapper.apply(i);
    }
    
}
