package de.zalando;

import com.google.common.reflect.TypeToken;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.parseMediaType;

public final class ResponseDispatcher {
    
    private final Supplier<List<HttpMessageConverter<?>>> converters;

    private ResponseDispatcher(Supplier<List<HttpMessageConverter<?>>> converters) {
        this.converters = converters;
    }

    public <O> ResponseExtractor<O> dispatch(Binding<?, O>... bindings) {
        return response -> {
            final MediaType contentType = response.getHeaders().getContentType();
            final Optional<Binding<Object, O>> match = findMatch(contentType, bindings);

            if (match.isPresent()) {
                final Binding<Object, O> binding = match.get();
                final Type type = binding.getType().getType();
                final ResponseExtractor extractor = new HttpMessageConverterExtractor<>(type, converters.get());
                final Object result = extractor.extractData(response);
                
                return binding.apply(result);
            } else {
                // TODO improve message
                throw new RestClientException("Can't dispatch response");
            }
        };
    }

    // TODO find best match, not first
    private <I, O> Optional<Binding<I, O>> findMatch(MediaType contentType, Binding<?, O>... bindings) {
        return asList(bindings).stream()
                .filter(b -> b.getContentType().includes(contentType))
                .map(this::<I, O>cast)
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private <I, O> Binding<I, O> cast(Binding<?, O> b) {
        return (Binding<I, O>) b;
    }

    public static <I> Binding<I, Void> handle(String contentType, Class<I> type, Consumer<I> consumer) {
        return handle(contentType, TypeToken.of(type), consumer);
    }

    public static <I> Binding<I, Void> handle(String contentType, TypeToken<I> type, Consumer<I> consumer) {
        return new Binding<>(parseMediaType(contentType), type, input -> {
            consumer.accept(input);
            return null;
        });
    }

    // TODO find better name
    @Deprecated
    public static <I, O> Binding<I, O> map(String contentType, Class<I> type, Function<I, O> mapper) {
        return map(contentType, TypeToken.of(type), mapper);
    }

    // TODO find better name
    @Deprecated
    public static <I, O> Binding<I, O> map(String contentType, TypeToken<I> type, Function<I, O> mapper) {
        return new Binding<>(parseMediaType(contentType), type, mapper);
    }
    
    public static ResponseDispatcher on(RestTemplate template) {
        return new ResponseDispatcher(template::getMessageConverters);
    }
    
}
