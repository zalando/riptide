package org.zalando.riptide.metrics;

import io.micrometer.core.instrument.Tag;
import org.apiguardian.api.API;
import org.springframework.http.client.ClientHttpResponse;
import org.zalando.riptide.RequestArguments;

import javax.annotation.Nullable;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public interface TagGenerator {

    Iterable<Tag> tags(RequestArguments arguments, @Nullable ClientHttpResponse response,
            @Nullable Throwable throwable);

}
