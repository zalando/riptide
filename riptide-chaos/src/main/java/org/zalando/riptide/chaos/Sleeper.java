package org.zalando.riptide.chaos;

import java.time.*;
import java.util.concurrent.*;

final class Sleeper {

    void sleep(final Duration duration) throws CancellationException {
        try {
            Thread.sleep(duration.toMillis());
        } catch (final InterruptedException e) {
            // preserve interrupt flag
            Thread.currentThread().interrupt();
            throw new CancellationException();
        }
    }

}
