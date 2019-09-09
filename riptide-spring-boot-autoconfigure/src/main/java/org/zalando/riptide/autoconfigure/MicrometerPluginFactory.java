package org.zalando.riptide.autoconfigure;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.zalando.riptide.Plugin;
import org.zalando.riptide.micrometer.MicrometerPlugin;

final class MicrometerPluginFactory {

    private MicrometerPluginFactory() {

    }

    public static Plugin createMicrometerPlugin(final MeterRegistry registry,
            final ImmutableList<Tag> tags) {
        return new MicrometerPlugin(registry).withDefaultTags(tags);
    }
}
