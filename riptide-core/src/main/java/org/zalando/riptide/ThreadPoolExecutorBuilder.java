package org.zalando.riptide;

import lombok.AllArgsConstructor;
import lombok.With;
import org.zalando.riptide.ThreadPoolExecutors.Builder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.defaultThreadFactory;
import static lombok.AccessLevel.PRIVATE;
import static org.zalando.riptide.ThreadPoolExecutors.Build;
import static org.zalando.riptide.ThreadPoolExecutors.CorePoolSize;
import static org.zalando.riptide.ThreadPoolExecutors.KeepAliveTime;
import static org.zalando.riptide.ThreadPoolExecutors.MaximumPoolSize;
import static org.zalando.riptide.ThreadPoolExecutors.RejectedExecutions;
import static org.zalando.riptide.ThreadPoolExecutors.Threads;
import static org.zalando.riptide.ThreadPoolExecutors.WorkQueue;

@With(PRIVATE)
@AllArgsConstructor
final class ThreadPoolExecutorBuilder implements
        Builder, WorkQueue, CorePoolSize, MaximumPoolSize, KeepAliveTime,
        Threads, RejectedExecutions, Build {

    private final Integer queueSize;
    private final Integer corePoolSize;
    private final Integer maximumPoolSize;
    private final Long keepAliveTime;
    private final TimeUnit unit;
    private final ThreadFactory threadFactory;
    private final RejectedExecutionHandler handler;

    public ThreadPoolExecutorBuilder() {
        this(0, 0, null, null, null,
                defaultThreadFactory(), new AbortPolicy());
    }

    @Override
    public MaximumPoolSize workQueue(final int queueSize) {
        return withQueueSize(queueSize);
    }

    @Override
    public MaximumPoolSize corePoolSize(final int corePoolSize) {
        return withCorePoolSize(corePoolSize);
    }

    @Override
    public KeepAliveTime maximumPoolSize(final int maximumPoolSize) {
        return withMaximumPoolSize(maximumPoolSize);
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
        if (queueSize == 0) {
            return new ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    unit,
                    new SynchronousQueue<>(),
                    threadFactory,
                    handler
            );
        } else {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    maximumPoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    unit,
                    new ArrayBlockingQueue<>(queueSize),
                    threadFactory,
                    handler
            );

            if (keepAliveTime > 0) {
                executor.allowCoreThreadTimeOut(true);
            }

            return executor;
        }
    }

}
