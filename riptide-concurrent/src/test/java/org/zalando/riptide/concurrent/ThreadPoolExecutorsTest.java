package org.zalando.riptide.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ThreadPoolExecutorsTest {

    @Test
    void withoutQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .fixedSize(1)
                .withoutQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isZero();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void withoutQueueElasticSizeMinSizeZero() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .withoutQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isZero();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void withoutQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .withoutQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
        assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void queueFirstEmptyBoundedQueueFails() {
        assertThrows(IllegalArgumentException.class, () ->
                ThreadPoolExecutors.builder()
                        .elasticSize(0, 1)
                        .keepAlive(Duration.ofMinutes(1))
                        .queueFirst()
                        .boundedQueue(0));
    }

    @Test
    void boundedQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .fixedSize(1)
                .boundedQueue(1)
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isZero();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void queueFirstBoundedQueueElasticSizeMinSizeZero() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .queueFirst()
                .boundedQueue(1)
                .build();

        assertThat(executor.getCorePoolSize()).isZero();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void queueFirstBoundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .queueFirst()
                .boundedQueue(1)
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
        assertThat(executor.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void queueFirstUnboundedQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .fixedSize(1)
                .unboundedQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(LinkedBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isZero();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void scaleFirstBoundedQueueElasticSizeMinSizeZero() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .boundedQueue(1)
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
    }

    @Test
    void scaleFirstBoundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .boundedQueue(1)
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
        assertThat(executor.getQueue()).isInstanceOf(WorkQueue.class);
        assertThat(((WorkQueue<Runnable>) executor.getQueue()).delegate())
                .isInstanceOf(ArrayBlockingQueue.class);
        assertThat(executor.getRejectedExecutionHandler())
                .isInstanceOf(ReEnqueuePolicy.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void scaleFirstUnboundedQueueElasticSizeMinSizeZero() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .unboundedQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isOne();
        assertThat(executor.getQueue()).isInstanceOf(LinkedBlockingQueue.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
    }

    @Test
    void scaleFirstUnboundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .unboundedQueue()
                .build();

        assertThat(executor.getCorePoolSize()).isOne();
        assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
        assertThat(executor.getQueue()).isInstanceOf(WorkQueue.class);
        assertThat(((WorkQueue<Runnable>) executor.getQueue()).delegate())
                .isInstanceOf(LinkedBlockingQueue.class);
        assertThat(executor.getRejectedExecutionHandler())
                .isInstanceOf(ReEnqueuePolicy.class);
        assertThat(((ReEnqueuePolicy) executor.getRejectedExecutionHandler()).getHandler())
                .isInstanceOf(AbortPolicy.class);
        assertThat(executor.getKeepAliveTime(TimeUnit.MINUTES)).isOne();
        assertThat(executor.allowsCoreThreadTimeOut()).isFalse();
    }

    @Test
    void preStartsThreads() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .fixedSize(1)
                .withoutQueue()
                .preStartThreads()
                .build();

        assertThat(executor.getPoolSize()).isEqualTo(1);
    }

    @Test
    void preStartsCoreThreads() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .unboundedQueue()
                .preStartThreads()
                .build();

        assertThat(executor.getPoolSize()).isEqualTo(1);
    }

    @Test
    void configuresThreadFactory() {
        final ThreadFactory threadFactory = new CustomThreadFactory();

        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .elasticSize(1, 2)
                .keepAlive(Duration.ofMinutes(1))
                .scaleFirst()
                .unboundedQueue()
                .threadFactory(threadFactory)
                .build();

        assertThat(executor.getThreadFactory()).isEqualTo(threadFactory);
    }

    private static final class CustomThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(final Runnable runnable) {
            return new Thread(runnable);
        }

    }

    @Test
    void configuresRejectedExecutionHandler() {
        final RejectedExecutionHandler handler = new CallerRunsPolicy();

        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .fixedSize(1)
                .withoutQueue()
                .handler(handler)
                .build();

        assertThat(executor.getRejectedExecutionHandler()).isEqualTo(handler);
    }

}
