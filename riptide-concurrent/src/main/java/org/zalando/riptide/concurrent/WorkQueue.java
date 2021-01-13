package org.zalando.riptide.concurrent;

import lombok.AllArgsConstructor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A special {@link BlockingQueue} that refuses to accept new elements
 * as long as no consumer is waiting.
 *
 * It deliberately breaks the {@link BlockingQueue} contract in order to
 * make any {@link ThreadPoolExecutor} using this queue start new threads
 * rather than adding tasks to the queue.
 *
 * Once the thread pool reaches it's {@link ThreadPoolExecutor#getMaximumPoolSize() maximum pool size}
 * it will then re-queue tasks using the {@link ReEnqueuePolicy}.
 *
 * @see <a href="https://medium.com/@uditharosha/java-scale-first-executorservice-4245a63222df">Java Scale First ExecutorService — A myth or a reality</a>
 * @see ReEnqueuePolicy
 * @param <E> element type
 */
@AllArgsConstructor
final class WorkQueue<E> extends ForwardingBlockingQueue<E> {

    private final AtomicInteger idleWorkers = new AtomicInteger();
    private final BlockingQueue<E> delegate;

    @Override
    protected BlockingQueue<E> delegate() {
        return delegate;
    }

    @Override
    public boolean offer(final E element) {
        if (idleWorkers.get() == 0) {
            return false;
        }

        return super.offer(element);
    }

    @Override
    public boolean add(final E element) {
        return super.offer(element);
    }

    @Override
    public E take() throws InterruptedException {
        idleWorkers.incrementAndGet();

        try {
            return super.take();
        } finally {
            idleWorkers.decrementAndGet();
        }
    }

    @Override
    public E poll(final long timeout, final TimeUnit unit)
            throws InterruptedException {

        idleWorkers.incrementAndGet();

        try {
            return super.poll(timeout, unit);
        } finally {
            idleWorkers.decrementAndGet();
        }
    }

}
