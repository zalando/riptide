package org.zalando.riptide.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.Collection;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;
import org.zalando.riptide.micrometer.tag.TagGenerator;

final class MicrometerPluginFactory {

    private MicrometerPluginFactory() {

    }

    public static Plugin create(
            final MeterRegistry registry,
            final Iterable<Tag> tags,
            final Collection<TagGenerator> generators) {

        return new MicrometerPlugin(registry)
                .withDefaultTags(tags)
                .withAdditionalTagGenerators(generators);
    }

}
