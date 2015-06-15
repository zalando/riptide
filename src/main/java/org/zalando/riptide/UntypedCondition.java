package org.zalando.riptide;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;

import static java.util.Arrays.asList;

public final class UntypedCondition<A> {
    
    private final Router router = new Router();
    private final Optional<A> attribute;

    public UntypedCondition(Optional<A> attribute) {
        this.attribute = attribute;
    }

    public Binding<A> call(ThrowingConsumer<ClientHttpResponse, IOException> consumer) {
        return Binding.create(attribute, (response, converters) -> {
            consumer.accept(response);
            return null;
        });
    }

    public Capturer<A> map(ThrowingFunction<ClientHttpResponse, ?, IOException> function) {
        return () -> Binding.create(attribute, (response, converters) -> function.apply(response));
    }

    public Binding<A> capture() {
        return Binding.create(attribute, (response, converters) -> response);
    }

    /**
     * 
     * @param selector
     * @param bindings
     * @param <B>
     * @return
     * @throws UnsupportedResponseException
     */
    @SafeVarargs
    public final <B> Binding<A> dispatch(Selector<B> selector, Binding<B>... bindings) {
        return Binding.create(attribute, (response, converters) ->
                router.route(response, converters, selector, asList(bindings)));
    }

}
