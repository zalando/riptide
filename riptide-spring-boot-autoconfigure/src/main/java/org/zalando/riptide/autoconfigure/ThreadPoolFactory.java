package org.zalando.riptide.autoconfigure;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.zalando.riptide.autoconfigure.RiptideProperties.Threads;
import org.zalando.riptide.concurrent.ThreadPoolExecutors.KeepAliveTime;

import java.util.concurrent.ThreadPoolExecutor;

import static org.zalando.riptide.concurrent.ThreadPoolExecutors.builder;

@SuppressWarnings("unused")
final class ThreadPoolFactory {

    private ThreadPoolFactory() {

    }

    public static ThreadPoolExecutor create(
            final String id,
            final Threads threads) {

        final TimeSpan keepAlive = threads.getKeepAlive();
        return configure(threads)
                .keepAlive(keepAlive.getAmount(), keepAlive.getUnit())
                .threadFactory(new CustomizableThreadFactory("http-" + id + "-"))
                .build();
    }

    private static KeepAliveTime configure(final Threads threads) {
        final int minSize = threads.getMinSize();
        final int maxSize = threads.getMaxSize();
        final int queueSize = threads.getQueueSize();

        if (queueSize == 0) {
            return builder()
                    .withoutQueue()
                    .elasticSize(minSize, maxSize);
        } else if (minSize == maxSize) {
            return builder()
                    .boundedQueue(queueSize)
                    .fixedSize(maxSize);
        } else {
            return builder()
                    .scaleFirst()
                    .boundedQueue(queueSize)
                    .elasticSize(maxSize);
        }
    }

}
