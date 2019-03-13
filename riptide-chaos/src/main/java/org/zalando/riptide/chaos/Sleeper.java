package org.zalando.riptide.chaos;

import java.time.Duration;
import java.util.concurrent.CancellationException;

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
