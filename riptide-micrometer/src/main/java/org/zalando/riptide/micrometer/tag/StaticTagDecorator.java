package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
@AllArgsConstructor
public final class StaticTagDecorator implements TagGenerator {

    private final Iterable<Tag> tags;

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return tags;
    }

}
