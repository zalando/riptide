package org.zalando.riptide.concurrent;

import lombok.AllArgsConstructor;
import lombok.With;
import org.zalando.riptide.concurrent.ThreadPoolExecutors.Builder;
import org.zalando.riptide.concurrent.ThreadPoolExecutors.FixedOrElasticSize;
import org.zalando.riptide.concurrent.ThreadPoolExecutors.FixedSize;
import org.zalando.riptide.concurrent.ThreadPoolExecutors.LimitedElasticSize;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.defaultThreadFactory;
import static lombok.AccessLevel.PRIVATE;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.Build;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.ElasticSize;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.KeepAliveTime;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.RejectedExecutions;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.ScaleFirst;
import static org.zalando.riptide.concurrent.ThreadPoolExecutors.Threads;

@With(PRIVATE)
@AllArgsConstructor
final class ThreadPoolExecutorBuilder implements
        Builder, ScaleFirst,
        FixedSize, ElasticSize, LimitedElasticSize, FixedOrElasticSize,
        KeepAliveTime, Threads, RejectedExecutions, Build {

    private final Integer corePoolSize;
    private final Integer maximumPoolSize;
    private final Long keepAliveTime;
    private final TimeUnit unit;
    private final boolean allowCoreThreadTimeOut;
    private final BlockingQueue<Runnable> queue;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler handler;

    public ThreadPoolExecutorBuilder() {
        this(null, null, null, null, false, null,
                defaultThreadFactory(), new AbortPolicy());
    }

    @Override
    public ScaleFirst scaleFirst() {
        return this;
    }

    @Override
    public ThreadPoolExecutorBuilder boundedQueue(final int queueSize) {
        return queueSize == 0 ?
                withQueue(new SynchronousQueue<>()) :
                withQueue(new ArrayBlockingQueue<>(queueSize));
    }

    @Override
    public ThreadPoolExecutorBuilder unboundedQueue() {
        return withQueue(new LinkedBlockingQueue<>());
    }

    @Override
    public KeepAliveTime fixedSize(final int poolSize) {
        return withCorePoolSize(poolSize)
                .withMaximumPoolSize(poolSize);
    }

    @Override
    public KeepAliveTime elasticSize(
            final int corePoolSize, final int maximumPoolSize) {

        return withCorePoolSize(corePoolSize)
                .withMaximumPoolSize(maximumPoolSize);
    }

    @Override
    public KeepAliveTime elasticSize(final int maximumPoolSize) {
        return withCorePoolSize(maximumPoolSize)
                .withMaximumPoolSize(maximumPoolSize)
                .withAllowCoreThreadTimeOut(true);
    }

    @Override
    public Threads keepAlive(final long time, final TimeUnit unit) {
        return withKeepAliveTime(time).withUnit(unit);
    }

    @Override
    public RejectedExecutions threadFactory(final ThreadFactory threadFactory) {
        return withThreadFactory(threadFactory);
    }

    @Override
    public Build handler(final RejectedExecutionHandler handler) {
        return withHandler(handler);
    }

    @Override
    public ThreadPoolExecutor build() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                queue,
                threadFactory,
                handler
        );

        if (keepAliveTime > 0) {
            executor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        }

        return executor;
    }

}
