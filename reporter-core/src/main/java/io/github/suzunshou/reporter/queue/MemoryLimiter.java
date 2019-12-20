package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.reporter.ReporterMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zunshou on 2019/11/18 10:33 下午.
 * Record total messages in queue
 */
public abstract class MemoryLimiter {

    private static final int MAX_TOTAL_MESSAGES = 1000000;

    public static MemoryLimiter create(int maxMessages, ReporterMetrics metrics) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages should be greater than 0.");
        }
        int result = Math.min(MAX_TOTAL_MESSAGES, maxMessages);
        return new DefaultMemoryLimiter(result, metrics);
    }

    /**
     * Returns true if exceeds memory limit
     */
    public abstract boolean isMaximum();

    /**
     * Block current thread if reach the limit until signaled
     */
    public abstract void blockWhenMaximum();

    /**
     * Signal all threads that waiting on this memory limiter
     */
    public abstract void signalAll();

    public static final class DefaultMemoryLimiter extends MemoryLimiter {
        private static final Logger logger = LoggerFactory.getLogger(DefaultMemoryLimiter.class);

        private final long maxMessages;
        private final ReporterMetrics metrics;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notFull = lock.newCondition();

        private DefaultMemoryLimiter(long maxMessages, ReporterMetrics metrics) {
            this.maxMessages = maxMessages;
            this.metrics = metrics;
        }

        @Override
        public boolean isMaximum() {
            return metrics.queuedMessages() >= maxMessages;
        }

        @Override
        public void blockWhenMaximum() {
            if (!isMaximum()) {
                lock.lock();
                try {
                    while (isMaximum()) {
                        notFull.await();
                    }
                } catch (InterruptedException e) {
                    logger.error("Interrupted waiting when full.");
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public void signalAll() {
            lock.lock();
            try {
                if (lock.hasWaiters(notFull) && !isMaximum()) {
                    notFull.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
