package io.github.suzunshou.reporter.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zunshou on 2019/11/18 10:37 上午.
 */
public class FlushSynchronizer {

    private final Queue<AbstractSizeBoundedQueue> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();

    /**
     * offer q to queue for flush.
     *
     * @param q
     * @return
     */
    public boolean offer(AbstractSizeBoundedQueue q) {
        if (q.isFull) {
            return false;
        }

        boolean result = queue.offer(q);

        if (lock.tryLock()) {
            try {
                if (lock.hasWaiters(notEmpty)) {
                    notEmpty.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }

        return result;
    }


    public AbstractSizeBoundedQueue poll(long timeoutNanos) {
        boolean interrupted = false;
        AbstractSizeBoundedQueue first;

        lock.lock();
        try {
            while ((first = queue.poll()) == null) {
                if (timeoutNanos <= 0L) {
                    return null;
                }
                try {
                    timeoutNanos = notEmpty.awaitNanos(timeoutNanos);
                } catch (InterruptedException e) {
                    interrupted = true;
                    break;
                }
            }
        } finally {
            lock.unlock();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return first;
    }

    public void release(AbstractSizeBoundedQueue queue) {
        queue.isFull = false;
    }

    public void clear(){
        queue.clear();
    }
}
