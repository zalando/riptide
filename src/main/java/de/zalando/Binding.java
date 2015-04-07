package de.zalando;

import com.google.common.reflect.TypeToken;
import org.springframework.http.MediaType;

import java.util.function.Function;

public final class Binding<I, O> implements Function<I, O> {

    private final MediaType contentType;
    private final TypeToken<I> type;
    private final Function<I, O> mapper;

    Binding(MediaType contentType, TypeToken<I> type, Function<I, O> mapper) {
        this.contentType = contentType;
        this.type = type;
        this.mapper = mapper;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public TypeToken<I> getType() {
        return type;
    }

    @Override
    public O apply(I i) {
        return mapper.apply(i);
    }
    
}
