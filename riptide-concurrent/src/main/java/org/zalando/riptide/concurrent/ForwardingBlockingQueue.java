package org.zalando.riptide.concurrent;

import lombok.experimental.Delegate;

import java.util.concurrent.BlockingQueue;

abstract class ForwardingBlockingQueue<E> implements BlockingQueue<E> {

    @Delegate
    protected abstract BlockingQueue<E> delegate();

}
