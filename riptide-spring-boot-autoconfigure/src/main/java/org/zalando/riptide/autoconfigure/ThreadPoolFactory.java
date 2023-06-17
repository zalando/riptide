package org.zalando.riptide.autoconfigure;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;
import org.zalando.riptide.concurrent.ThreadPoolExecutors;

import java.util.concurrent.ThreadPoolExecutor;

import static org.zalando.riptide.concurrent.ThreadPoolExecutors.builder;

@SuppressWarnings("unused")
final class ThreadPoolFactory {

    private ThreadPoolFactory() {

    }

    public static ThreadPoolExecutor create(
            final String id,
            final Threads threads) {

        return configure(threads)
                .threadFactory(new CustomizableThreadFactory("http-" + id + "-"))
                .build();
    }

    private static ThreadPoolExecutors.Threads configure(final Threads threads) {
        final int minSize = threads.getMinSize();
        final int maxSize = threads.getMaxSize();
        final int queueSize = threads.getQueueSize();
        final TimeSpan keepAlive = threads.getKeepAlive();

        if (queueSize == 0) {
            return builder()
                    .elasticSize(minSize, maxSize)
                    .keepAlive(keepAlive.getAmount(), keepAlive.getUnit())
                    .withoutQueue();
        } else if (minSize == maxSize) {
            return builder()
                    .fixedSize(maxSize)
                    .boundedQueue(queueSize);
        } else {
            return builder()
                    .elasticSize(minSize, maxSize)
                    .keepAlive(keepAlive.getAmount(), keepAlive.getUnit())
                    .scaleFirst()
                    .boundedQueue(queueSize);
        }
    }

}
