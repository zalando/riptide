package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.Collections;

import static java.util.stream.StreamSupport.stream;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

@AllArgsConstructor
final class CompositeTagGenerator implements TagGenerator {

    private final Iterable<TagGenerator> generators;

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {

        return stream(generators.spliterator(), false)
                .map(generator -> generator.onRequest(arguments))
                .reduce(Tags::concat)
                .orElse(Collections.emptyList());
    }

    @Override
    public Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) {

        return stream(generators.spliterator(), false)
                .map(throwingFunction(generator ->
                        generator.onResponse(arguments, response)))
                .reduce(Tags::concat)
                .orElse(Collections.emptyList());
    }

    @Override
    public Iterable<Tag> onError(
            final RequestArguments arguments,
            final Throwable throwable) {

        return stream(generators.spliterator(), false)
                .map(generator -> generator.onError(arguments, throwable))
                .reduce(Tags::concat)
                .orElse(Collections.emptyList());
    }

}
