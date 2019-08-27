package org.zalando.riptide.chaos;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SleeperTest {

    private final Sleeper unit = new Sleeper();

    @Test
    void sleep() {
        final Clock clock = Clock.systemUTC();
        final Instant start = clock.instant();

        unit.sleep(Duration.ofSeconds(1));

        final Instant end = clock.instant();

        assertThat(Duration.between(start, end), is(greaterThanOrEqualTo(Duration.ofSeconds(1))));
    }

    @Test
    void shouldInterrupt() throws InterruptedException {
        final AtomicReference<CancellationException> exception = new AtomicReference<>();

        final Thread thread = new Thread(() ->
                exception.set(assertThrows(CancellationException.class, () ->
                        unit.sleep(Duration.ofSeconds(1)))));

        thread.start();
        thread.interrupt();
        thread.join();

        assertThat(exception.get(), is(instanceOf(CancellationException.class)));
    }

}
