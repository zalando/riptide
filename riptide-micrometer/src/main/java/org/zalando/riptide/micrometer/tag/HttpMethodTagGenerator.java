package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class HttpMethodTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        return singleton(Tag.of("http.method", arguments.getMethod().name()));
    }
}
