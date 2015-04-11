package de.zalando;

import org.springframework.http.client.ClientHttpResponse;

import java.util.Map;
import java.util.Optional;

public interface Selector<A> {

    A attributeOf(ClientHttpResponse response);
    
    default <O> Optional<Binding<A, ?, O>> select(A attribute, Map<A, Binding<A, ?, O>> bindings) {
        return Optional.ofNullable(bindings.get(attribute));
    }

}
