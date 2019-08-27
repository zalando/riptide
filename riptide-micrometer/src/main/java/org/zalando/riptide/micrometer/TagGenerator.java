package org.zalando.riptide.micrometer;

import io.micrometer.core.instrument.*;
import org.apiguardian.api.*;
import org.springframework.http.client.*;
import org.zalando.riptide.*;

import javax.annotation.*;

import static org.apiguardian.api.API.Status.*;

@API(status = EXPERIMENTAL)
public interface TagGenerator {

    Iterable<Tag> tags(RequestArguments arguments, @Nullable ClientHttpResponse response,
            @Nullable Throwable throwable);

}
