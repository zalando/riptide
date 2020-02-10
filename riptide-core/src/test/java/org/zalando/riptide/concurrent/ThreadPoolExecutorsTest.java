package org.zalando.riptide.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

final class ThreadPoolExecutorsTest {

    @Test
    void withoutQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .withoutQueue()
                .fixedSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(SynchronousQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void withoutQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .withoutQueue()
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(0));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(SynchronousQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void emptyBoundedQueueEqualsNoQueue() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .boundedQueue(0)
                .fixedSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(SynchronousQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void boundedQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .boundedQueue(1)
                .fixedSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(ArrayBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void boundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .boundedQueue(1)
                .elasticSize(0, 1)
                .keepAlive(Duration.ofMinutes(1))
                .threadFactory(Executors.defaultThreadFactory())
                .handler(new CallerRunsPolicy())
                .build();

        assertThat(executor.getCorePoolSize(), is(0));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(ArrayBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void unboundedQueueFixedSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .unboundedQueue()
                .fixedSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(LinkedBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

    @Test
    void scaleFirstBoundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .scaleFirst()
                .boundedQueue(1)
                .elasticSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(ArrayBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(true));
    }

    @Test
    void scaleFirstUnboundedQueueElasticSize() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .scaleFirst()
                .unboundedQueue()
                .elasticSize(1)
                .keepAlive(Duration.ofMinutes(1))
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(LinkedBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(true));
    }

    @Test
    void scaleFirstUnboundedQueueElasticSizeWithoutKeepAlive() {
        final ThreadPoolExecutor executor = ThreadPoolExecutors.builder()
                .scaleFirst()
                .unboundedQueue()
                .elasticSize(1)
                .keepAlive(Duration.ZERO)
                .build();

        assertThat(executor.getCorePoolSize(), is(1));
        assertThat(executor.getMaximumPoolSize(), is(1));
        assertThat(executor.getQueue(), is(instanceOf(LinkedBlockingQueue.class)));
        assertThat(executor.allowsCoreThreadTimeOut(), is(false));
    }

}
