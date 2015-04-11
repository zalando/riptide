package de.zalando;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Map;
import java.util.Optional;

final class ContentTypeSelector implements Selector<MediaType> {

    @Override
    public MediaType attributeOf(ClientHttpResponse response) {
        return response.getHeaders().getContentType();
    }

    @Override
    public <O> Optional<Binding<MediaType, ?, O>> select(MediaType contentType, Map<MediaType, Binding<MediaType, ?, O>> bindings) {
        // TODO find best match, not first
        return bindings.entrySet().stream()
                .filter(e -> e.getKey().includes(contentType))
                .findFirst()
                .map(Map.Entry::getValue);
    }

}
