package org.zalando.riptide.compatibility;

import org.springframework.web.client.*;
import org.zalando.riptide.*;
import org.zalando.riptide.capture.*;

import javax.annotation.*;

import static org.zalando.fauxpas.TryWith.*;

final class ExtractRoute {

    private ExtractRoute() {

    }

    static <T> Route extractTo(@Nullable final ResponseExtractor<T> extractor, final Capture<T> capture) {
        return (response, reader) ->
                tryWith(response, ignored -> {
                    if (extractor == null) {
                        capture.capture(null);
                    } else {
                        capture.capture(extractor.extractData(response));
                    }
                });
    }

}
