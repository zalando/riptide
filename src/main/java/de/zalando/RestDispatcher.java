package de.zalando;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public final class RestDispatcher<A> {

    private final Supplier<List<HttpMessageConverter<?>>> converters;
    private final Selector<A> selector;

    private RestDispatcher(Supplier<List<HttpMessageConverter<?>>> converters, Selector<A> selector) {
        this.converters = converters;
        this.selector = selector;
    }

    // TODO require at least one binding? via signature or validation
    @SafeVarargs
    public final <O> ResponseExtractor<O> dispatch(Binding<A, ? extends Object, O>... elements) {
        final List<Binding<A, ?, O>> bindings = asList(elements);
        
        ensureUniqueAttributeValues(bindings);

        return response -> {
            final A attribute = selector.attributeOf(response);
            final Map<A, Binding<A, ?, O>> mapping = bindings.stream()
                    .collect(toMap(Binding::getAttribute, identity()));
            final Optional<Binding<A, Object, O>> match = selector.select(attribute, mapping).map(this::cast);

            if (match.isPresent()) {
                final Binding<A, Object, O> binding = match.get();

                final Object input = getOutput(response, binding);

                // TODO support new ResponseEntity<T>(body, response.getHeaders(), response.getStatusCode())
                return binding.apply(input);
            } else {
                // TODO improve message
                throw new RestClientException("Can't dispatch response");
            }
        };
    }

    // TODO test
    // TODO document
    private <O> void ensureUniqueAttributeValues(Collection<Binding<A, ?, O>> bindings) {
        final List<A> duplicates = bindings.stream()
                .map(Binding::getAttribute)
                .collect(groupingBy(identity(), counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(toList());

        if (duplicates.isEmpty()) {
            return;
        }
        
        throw new IllegalArgumentException(
                format("Binding attributes have to be unique, got duplicates: %s", duplicates));
    }

    private <I, O> I getOutput(ClientHttpResponse response, Binding<A, I, O> binding) throws java.io.IOException {
        final Type type = binding.getType();
        final ResponseExtractor<I> extractor = new HttpMessageConverterExtractor<>(type, converters.get());
        return extractor.extractData(response);
    }

    @SuppressWarnings("unchecked")
    private <O> Binding<A, Object, O> cast(Binding<A, ?, O> b) {
        return (Binding<A, Object, O>) b;
    }

    public static <A> RestDispatcher<A> on(RestTemplate template, Selector<A> selector) {
        return new RestDispatcher<>(template::getMessageConverters, selector);
    }

    public static Selector<MediaType> contentType() {
        return new ContentTypeSelector();
    }

    public static Selector<HttpStatus> statusCode() {
        return new StatusCodeSelector();
    }

    public static <A, I> Binding<A, I, Void> handle(A attribute, Class<I> type, Consumer<I> consumer) {
        return new Binding<>(attribute, type, input -> {
            consumer.accept(input);
            return null;
        });
    }

    public static <A, I> Binding<A, I, Void> handle(A attribute, ParameterizedTypeReference<I> type, Consumer<I> consumer) {
        return new Binding<>(attribute, type.getType(), input -> {
            consumer.accept(input);
            return null;
        });
    }

    // TODO find better name
    public static <A, I, O> Binding<A, I, O> map(A attribute, Class<I> type, Function<I, O> mapper) {
        return new Binding<>(attribute, type, mapper);
    }

    // TODO find better name
    public static <A, I, O> Binding<A, I, O> map(A attribute, ParameterizedTypeReference<I> type, Function<I, O> mapper) {
        return new Binding<>(attribute, type.getType(), mapper);
    }

}
