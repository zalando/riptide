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
    public interface Builder {

        default FixedOrElasticSize withoutQueue() {
            return boundedQueue(0);
        }

        FixedOrElasticSize boundedQueue(int queueSize);
        FixedSize unboundedQueue();
        ScaleFirst scaleFirst();

    }

    @CheckReturnValue
    public interface ScaleFirst {
        LimitedElasticSize boundedQueue(int queueSize);
        LimitedElasticSize unboundedQueue();
    }

    public interface FixedOrElasticSize extends FixedSize, ElasticSize {

    }

    @CheckReturnValue
    public interface FixedSize {
        KeepAliveTime fixedSize(int poolSize);
    }

    @CheckReturnValue
    public interface ElasticSize {
        KeepAliveTime elasticSize(int corePoolSize, int maximumPoolSize);
    }

    @CheckReturnValue
    public interface LimitedElasticSize {
        KeepAliveTime elasticSize(int maximumPoolSize);
    }

    @CheckReturnValue
    public interface KeepAliveTime {

        default Threads keepAlive(final Duration duration) {
            return keepAlive(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        Threads keepAlive(long time, TimeUnit unit);

    }

    @CheckReturnValue
    public interface Threads extends Build {
        RejectedExecutions threadFactory(ThreadFactory threadFactory);
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

    public static Builder builder() {
        return new ThreadPoolExecutorBuilder();
    }

}
