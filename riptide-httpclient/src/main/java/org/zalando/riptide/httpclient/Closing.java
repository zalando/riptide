package org.zalando.riptide.httpclient;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;

@Slf4j
final class Closing {

    private Closing() {
        // nothing to do
    }

    @VisibleForTesting
    static void closeQuietly(@Nullable final Object object) {
        if (object instanceof Closeable) {
            try {
                ((Closeable) object).close();
            } catch (final IOException e) {
                log.warn("IOException thrown while closing Closeable.", e);
            }
        }
    }

}
