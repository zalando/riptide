package org.zalando.riptide;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.zalando.fauxpas.FauxPas.throwingRunnable;

final class ThreadPoolExecutorsTest {

    @Test
    void allowsNoQueue() {
        verifyNoQueue(ThreadPoolExecutors.builder()
                .corePoolSize(0)
                .maximumPoolSize(1)
                .keepAlive(0, TimeUnit.MINUTES)
                .threadFactory(Executors.defaultThreadFactory())
                .handler(new AbortPolicy())
                .build());
    }

    @Test
    void allowsQueueSizeZero() {
        verifyNoQueue(ThreadPoolExecutors.builder()
                .workQueue(0)
                .maximumPoolSize(1)
                .keepAlive(0, TimeUnit.MINUTES)
                .threadFactory(Executors.defaultThreadFactory())
                .handler(new AbortPolicy())
                .build());
    }

    private void verifyNoQueue(final ThreadPoolExecutor executor) {
        try {
            final CountDownLatch latch = new CountDownLatch(1);

            executor.execute(throwingRunnable(latch::await));
            assertThrows(RejectedExecutionException.class, () ->
                    executor.execute(() -> {}));

            latch.countDown();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void scalesFirst() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .workQueue(1)
                .maximumPoolSize(2)
                .keepAlive(1, TimeUnit.MINUTES)
                .build();

        verifyQueue(executor);
    }

    @Test
    void supportsKeepAliveTimeOfZero() {
        verifyQueue(ThreadPoolExecutors.builder()
                .workQueue(1)
                .maximumPoolSize(2)
                .keepAlive(0, TimeUnit.MINUTES)
                .build());
    }

    private void verifyQueue(final ThreadPoolExecutor executor) {
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            assertEquals(0, executor.getPoolSize());
            assertEquals(0, executor.getQueue().size());

            executor.execute(throwingRunnable(latch::await));
            executor.execute(throwingRunnable(latch::await));

            assertEquals(2, executor.getPoolSize());
            assertEquals(0, executor.getQueue().size());

            executor.execute(throwingRunnable(latch::await));

            assertEquals(2, executor.getPoolSize());
            assertEquals(1, executor.getQueue().size());

            latch.countDown();
        } finally {
            executor.shutdown();
        }
    }

}
