package org.zalando.riptide.autoconfigure;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.zalando.riptide.ThreadPoolExecutors.MaximumPoolSize;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;

import java.util.concurrent.ThreadPoolExecutor;

import static org.zalando.riptide.ThreadPoolExecutors.builder;

@SuppressWarnings("unused")
final class ThreadPoolFactory {

    private ThreadPoolFactory() {

    }

    public static ThreadPoolExecutor create(
            final String id,
            final Threads threads) {

        final TimeSpan keepAlive = threads.getKeepAlive();
        return configure(threads)
                .maximumPoolSize(threads.getMaxSize())
                .keepAlive(keepAlive.getAmount(), keepAlive.getUnit())
                .threadFactory(new CustomizableThreadFactory("http-" + id + "-"))
                .build();
    }

    private static MaximumPoolSize configure(final Threads threads) {
        final int queueSize = threads.getQueueSize();
        return queueSize == 0 ?
                builder().corePoolSize(threads.getMinSize()) :
                builder().workQueue(queueSize);
    }

}
