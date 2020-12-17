package org.zalando.riptide.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("UnstableApiUsage")
final class ScaleFirstTest {

    @Test
    void scalesToMaximumBeforeQueuing() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .unboundedQueue()
                .build();

        assertThat(unit.getPoolSize()).isZero();
        assertThat(unit.getQueue()).isEmpty();

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(1);
        assertThat(unit.getQueue()).isEmpty();

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(2);
        assertThat(unit.getQueue()).isEmpty();

        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(2);
        assertThat(unit.getQueue()).hasSize(1);

        unit.shutdownNow();
    }

    @Test
    void rejectsWhenQueueIsFull() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .boundedQueue(1)
                .build();

        unit.execute(sleep());
        unit.execute(sleep());
        unit.execute(sleep());

        assertThrows(RejectedExecutionException.class, () ->
            unit.execute(sleep()));
    }

    @Test
    void discardsWhenQueueIsFull() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .boundedQueue(1)
                .handler(new DiscardPolicy())
                .build();

        unit.execute(sleep());
        unit.execute(sleep());
        unit.execute(sleep());
        unit.execute(sleep());

        assertThat(unit.getPoolSize()).isEqualTo(2);
        assertThat(unit.getQueue()).hasSize(1);
    }

    @Test
    void takes() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMillis(100))
                .scaleFirst()
                .boundedQueue(1)
                .preStartThreads()
                .build();

        sleepUninterruptibly(ofMillis(50));

        unit.execute(sleep(ZERO));

        sleepUninterruptibly(ofMillis(200));
        assertThat(unit.getPoolSize()).isOne();
    }

    @Test
    void polls() {
        final ThreadPoolExecutor unit = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMillis(100))
                .scaleFirst()
                .boundedQueue(1)
                .preStartThreads()
                .build();

        unit.allowCoreThreadTimeOut(true);

        sleepUninterruptibly(ofMillis(200));
        assertThat(unit.getPoolSize()).isZero();
    }

    private Runnable sleep() {
        return sleep(Duration.ofMinutes(1));
    }

    private Runnable sleep(final Duration duration) {
        return () -> sleepUninterruptibly(duration);
    }

}
