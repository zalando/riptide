package org.zalando.riptide.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import java.io.IOException;

import static java.util.Collections.singleton;
import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class HttpStatusTagGenerator implements TagGenerator {

    @Override
    public Iterable<Tag> onResponse(
            final RequestArguments arguments,
            final ClientHttpResponse response) throws IOException {

        final String status = String.valueOf(response.getRawStatusCode());
        return singleton(Tag.of("http.status_code", status));
    }

}
