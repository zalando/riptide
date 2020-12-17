package org.zalando.riptide.concurrent;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static lombok.AccessLevel.PACKAGE;

@AllArgsConstructor
final class ReEnqueuePolicy implements RejectedExecutionHandler {

    @Getter(PACKAGE) // visible for testing
    private final RejectedExecutionHandler handler;

    @Override
    public void rejectedExecution(
            final Runnable task, final ThreadPoolExecutor executor) {

        final boolean added = executor.getQueue().add(task);

        if (!added) {
            handler.rejectedExecution(task, executor);
        }
    }

}

