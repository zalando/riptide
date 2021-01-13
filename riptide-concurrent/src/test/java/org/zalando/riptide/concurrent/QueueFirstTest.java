package org.zalando.riptide.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class QueueFirstTest {

    @Test
    void queuesToMaximumBeforeScaling() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .queueFirst()
                .boundedQueue(10)
                .build();

        assertThat(unit.getPoolSize()).isZero();
        assertThat(unit.getQueue()).isEmpty();

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(1);
        assertThat(unit.getQueue()).isEmpty();

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(1);
        assertThat(unit.getQueue()).hasSize(1);

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(1);
        assertThat(unit.getQueue()).hasSize(2);

        unit.shutdownNow();
    }

    private Runnable sleep() {
        return sleep(Duration.ofMinutes(1));
    }

    @SuppressWarnings("UnstableApiUsage")
    private Runnable sleep(final Duration duration) {
        return throwingRunnable(() -> sleepUninterruptibly(duration));
    }

}
