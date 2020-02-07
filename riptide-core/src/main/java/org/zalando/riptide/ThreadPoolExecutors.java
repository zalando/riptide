package org.zalando.riptide;

import org.apiguardian.api.API;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

@API(status = EXPERIMENTAL)
public final class ThreadPoolExecutors {

    public interface Builder extends CorePoolSize, WorkQueue {

    }

    public interface WorkQueue {
        MaximumPoolSize workQueue(int queueSize);
    }

    public interface CorePoolSize {
        MaximumPoolSize corePoolSize(int corePoolSize);
    }

    public interface MaximumPoolSize {
        KeepAliveTime maximumPoolSize(int maxSize);
    }

    public interface KeepAliveTime {
        Threads keepAlive(long time, TimeUnit unit);
    }

    public interface Threads extends Build {
        RejectedExecutions threadFactory(ThreadFactory threadFactory);
    }

    public interface RejectedExecutions extends Build {
        Build handler(RejectedExecutionHandler handler);
    }

    public interface Build {
        ThreadPoolExecutor build();
    }

    private ThreadPoolExecutors() {

    }

    public static Builder builder() {
        return new ThreadPoolExecutorBuilder();
    }

}
