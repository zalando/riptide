package org.zalando.riptide.concurrent;

import org.apiguardian.api.API;

import javax.annotation.CheckReturnValue;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class ThreadPoolExecutors {

    @CheckReturnValue
    public interface Start {
        FixedSize fixedSize(int poolSize);
        ElasticSize elasticSize(int corePoolSize, int maximumPoolSize);
    }

    @CheckReturnValue
    public interface FixedSize {
        Threads withoutQueue();
        Threads boundedQueue(int queueSize);
        Threads unboundedQueue();
    }

    @CheckReturnValue
    public interface ElasticSize {

        default KeepAliveTime keepAlive(final Duration duration) {
            return keepAlive(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        KeepAliveTime keepAlive(long time, TimeUnit unit);

    }

    @CheckReturnValue
    public interface KeepAliveTime {
        Threads withoutQueue();
        QueueFirst queueFirst();
        ScaleFirst scaleFirst();
    }

    @CheckReturnValue
    public interface QueueFirst{
        Threads boundedQueue(int queueSize);
    }

    @CheckReturnValue
    public interface ScaleFirst {
        Threads boundedQueue(int queueSize);
        Threads unboundedQueue();
    }

    @CheckReturnValue
    public interface Threads extends PreStart {
        PreStart threadFactory(ThreadFactory threadFactory);
    }

    @CheckReturnValue
    public interface PreStart extends RejectedExecutions {
        default RejectedExecutions preStartThreads() {
            return preStartThreads(true);
        }

        RejectedExecutions preStartThreads(boolean preStartThreads);
    }

    @CheckReturnValue
    public interface RejectedExecutions extends Build {
        Build handler(RejectedExecutionHandler handler);
    }

    @CheckReturnValue
    public interface Build {
        ThreadPoolExecutor build();
    }

    private ThreadPoolExecutors() {

    }

    @CheckReturnValue
    public static Start builder() {
        return new ThreadPoolExecutorBuilder();
    }

}
