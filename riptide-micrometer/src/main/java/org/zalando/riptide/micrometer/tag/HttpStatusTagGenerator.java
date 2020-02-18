package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class HttpStatusTagGenerator implements TagGenerator {

    private static final String STATUS_CODE = "http.status_code";
    private static final Set<Tag> NONE = singleton(Tag.of(STATUS_CODE, "0"));

    @Override
    public Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) throws IOException {

        final String status = String.valueOf(response.getRawStatusCode());
        return singleton(Tag.of(STATUS_CODE, status));
    }

    @Override
    public Iterable<Tag> onError(
            final RequestArguments arguments,
            final Throwable throwable) {

        return NONE;
    }

}
