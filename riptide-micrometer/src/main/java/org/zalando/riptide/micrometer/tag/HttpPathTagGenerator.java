package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class HttpPathTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onRequest(final RequestArguments arguments) {
        @Nullable final String uriTemplate = arguments.getUriTemplate();
        return singleton(Tag.of("http.path", firstNonNull(uriTemplate, "")));
    }

}
