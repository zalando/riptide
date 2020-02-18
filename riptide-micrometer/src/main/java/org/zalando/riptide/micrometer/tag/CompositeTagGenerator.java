package org.zalando.riptide.micrometer.tag;

import com.google.common.collect.Iterables;
import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

@AllArgsConstructor
final class CompositeTagGenerator implements TagGenerator {

    private final Iterable<TagGenerator> generators;

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return generators()
                .map(generator -> generator.onRequest(arguments))
                .collect(collectingAndThen(toList(), Iterables::concat));
    }

    @Override
    public Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) {

        return generators()
                .map(throwingFunction(generator ->
                        generator.onResponse(arguments, response)))
                .collect(collectingAndThen(toList(), Iterables::concat));
    }

    @Override
    public Iterable<Tag> onError(
            final RequestArguments arguments,
            final Throwable throwable) {

        return generators()
                .map(generator -> generator.onError(arguments, throwable))
                .collect(collectingAndThen(toList(), Iterables::concat));
    }

    private Stream<TagGenerator> generators() {
        return stream(generators.spliterator(), false);
    }

}
