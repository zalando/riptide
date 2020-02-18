package org.zalando.riptide.micrometer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.fauxpas.ThrowingSupplier;
import org.zalando.riptide.Attribute;
import org.zalando.riptide.AttributeStage;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.RequestArguments;
import org.zalando.riptide.RequestExecution;
import org.zalando.riptide.micrometer.tag.CallSiteTagGenerator;
import org.zalando.riptide.micrometer.tag.ErrorKindTagGenerator;
import org.zalando.riptide.micrometer.tag.HttpMethodTagGenerator;
import org.zalando.riptide.micrometer.tag.HttpPathTagGenerator;
import org.zalando.riptide.micrometer.tag.HttpStatusTagGenerator;
import org.zalando.riptide.micrometer.tag.PeerHostnameTagGenerator;
import org.zalando.riptide.micrometer.tag.ServiceLoaderTagGenerator;
import org.zalando.riptide.micrometer.tag.TagGenerator;

import java.io.IOException;
import java.util.Collection;

import static com.google.common.collect.ImmutableList.copyOf;
import static io.micrometer.core.instrument.Timer.builder;
import static lombok.AccessLevel.PRIVATE;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;
import static org.zalando.riptide.micrometer.CompletableFutures.onError;
import static org.zalando.riptide.micrometer.CompletableFutures.onResult;

@API(status = EXPERIMENTAL)
@AllArgsConstructor(access = PRIVATE)
public final class MicrometerPlugin implements Plugin {

    /**
     * Allows to pass arbitrary metrics tags directly from a call site.
     *
     * @see AttributeStage#attribute(Attribute, Object)
     */
    public static final Attribute<Tags> TAGS = Attribute.generate();

    private final MeterRegistry registry;
    private final String metricName;
    private final ImmutableList<Tag> defaultTags;
    private final TagGenerator generator;

    public MicrometerPlugin(final MeterRegistry registry) {
        this(registry,
                "http.client.requests",
                ImmutableList.of(),
                TagGenerator.composite(
                        new CallSiteTagGenerator(),
                        new ErrorKindTagGenerator(),
                        new HttpMethodTagGenerator(),
                        new HttpPathTagGenerator(),
                        new HttpStatusTagGenerator(),
                        new PeerHostnameTagGenerator(),
                        new ServiceLoaderTagGenerator()
                ));
    }

    public MicrometerPlugin withMetricName(final String metricName) {
        return new MicrometerPlugin(registry, metricName, defaultTags, generator);
    }

    public MicrometerPlugin withDefaultTags(final Tag... defaultTags) {
        return withDefaultTags(copyOf(defaultTags));
    }

    public MicrometerPlugin withDefaultTags(final Iterable<Tag> defaultTags) {
        return new MicrometerPlugin(registry, metricName, copyOf(defaultTags), generator);
    }

    public MicrometerPlugin withAdditionalTagGenerators(
            final TagGenerator first, final TagGenerator... rest) {
        return withAdditionalTagGenerators(Lists.asList(first, rest));
    }

    public MicrometerPlugin withAdditionalTagGenerators(
            final Collection<TagGenerator> generators) {
        return withTagGenerators(TagGenerator.composite(
                generator, TagGenerator.composite(generators)));
    }

    public MicrometerPlugin withTagGenerators(
            final TagGenerator generator, final TagGenerator... generators) {
        return withTagGenerators(Lists.asList(generator, generators));
    }

    public MicrometerPlugin withTagGenerators(
            final Collection<TagGenerator> generators) {
        return new MicrometerPlugin(registry, metricName, defaultTags,
                TagGenerator.composite(generators));
    }

    @Override
    public RequestExecution aroundNetwork(final RequestExecution execution) {
        return arguments -> {
            final Measurement measurement = new Measurement(arguments);

            return execution.execute(arguments)
                    .whenComplete(onResult(measurement::record))
                    .whenComplete(onError(measurement::record));
        };
    }

    @AllArgsConstructor
    private final class Measurement {

        private final Sample sample = Timer.start(registry);
        private final RequestArguments arguments;

        void record(final ClientHttpResponse response) throws IOException {
            record(() -> generator.onResponse(arguments, response));
        }

        void record(final Throwable throwable) {
            record(() -> generator.onError(arguments, throwable));
        }

        <X extends Exception> void record(
                final ThrowingSupplier<Iterable<Tag>, X> tags) throws X {

            final Timer timer = builder(metricName)
                    .tags(defaultTags)
                    .tags(generator.onRequest(arguments))
                    .tags(tags.tryGet())
                    .register(registry);

            sample.stop(timer);
        }

    }

}
