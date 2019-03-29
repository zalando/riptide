package org.zalando.riptide.compatibility;

import org.springframework.web.client.ResponseExtractor;
import org.zalando.riptide.Route;
import org.zalando.riptide.capture.Capture;

import javax.annotation.Nullable;

import static org.zalando.fauxpas.TryWith.tryWith;

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
