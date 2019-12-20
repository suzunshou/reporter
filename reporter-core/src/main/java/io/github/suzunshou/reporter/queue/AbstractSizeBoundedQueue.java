package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.buffers.BufferFilter;
import io.github.suzunshou.reporter.concurrent.MessagePromise;
import io.github.suzunshou.reporter.reporter.Message;

/**
 * @author zunshou on 2019/11/17 5:27 下午.
 */
public abstract class AbstractSizeBoundedQueue {

    volatile boolean isFull = false;

    private volatile long lastAccessNanos = System.nanoTime();

    final int maxSize;
    public final Message.MessageKey key;

    AbstractSizeBoundedQueue(int maxSize, Message.MessageKey key) {
        this.maxSize = maxSize;
        this.key = key;
    }

    /**
     * offer messages to queue.
     *
     * @param promise
     */
    public abstract void offer(MessagePromise<?> promise);

    /**
     * remove message and send it to MessageFilter.
     *
     * @param filter
     * @return
     */
    public abstract int drainTo(BufferFilter<MessagePromise<?>> filter);

    /**
     * clear all messages in the queue.
     *
     * @return
     */
    public abstract int clear();

    /**
     * return the size of queue.
     *
     * @return
     */
    public abstract int size();

    /**
     * Return {@code true} if empty.
     *
     * @return
     */
    public abstract boolean isEmpty();

    void recordAccess() {
        lastAccessNanos = System.nanoTime();
    }

    long getLastAccessNanos() {
        return lastAccessNanos;
    }
}
