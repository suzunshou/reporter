package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.concurrent.timer.HashedWheelTimer;
import io.github.suzunshou.reporter.concurrent.timer.TimeDriven;
import io.github.suzunshou.reporter.concurrent.timer.Timeout;
import io.github.suzunshou.reporter.concurrent.timer.TimerTask;
import io.github.suzunshou.reporter.reporter.Message;
import io.github.suzunshou.reporter.reporter.ReporterMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/17 6:56 下午.
 */
public class QueueManager {
    private static final Logger logger = LoggerFactory.getLogger(QueueManager.class);

    private final ConcurrentHashMap<Message.MessageKey, AbstractSizeBoundedQueue> keyToQueue;

    private final int queuedMaxMessages;
    private final OverflowStrategy.Type overflowStrategy;
    private final long queuedKeepAliveNanos;

    private final TimeDriven<Message.MessageKey> timeDriven;
    private final ReporterMetrics metrics;
    private final HashedWheelTimer timer;

    private final SizeBoundedQueueFactory queueFactory = SizeBoundedQueueFactory.factory();
    private CreateCallback createCallback;

    public QueueManager(int queuedMaxMessages, OverflowStrategy.Type overflowStrategy, long queuedKeepAliveNanos,
                        TimeDriven<Message.MessageKey> timeDriven, ReporterMetrics metrics, HashedWheelTimer timer) {
        this.keyToQueue = new ConcurrentHashMap<>();

        this.queuedMaxMessages = queuedMaxMessages;
        this.overflowStrategy = overflowStrategy;
        this.queuedKeepAliveNanos = queuedKeepAliveNanos;

        this.timeDriven = timeDriven;
        this.metrics = metrics;
        this.timer = timer;
    }

    /**
     * get queue by key.
     *
     * @param key
     * @return
     */
    public AbstractSizeBoundedQueue get(Message.MessageKey key) {
        return keyToQueue.get(key);
    }

    /**
     * getOrCreate queue by key.
     *
     * @param key
     * @return
     */
    public AbstractSizeBoundedQueue getOrCreate(Message.MessageKey key) {
        AbstractSizeBoundedQueue queue = get(key);
        if (queue == null) {
            queue = queueFactory.newSizeBoundedQueue(queuedMaxMessages, overflowStrategy, key);
            AbstractSizeBoundedQueue prev = keyToQueue.putIfAbsent(key, queue);

            if (prev == null) {
                timer.newTimeout(new CleanTask(queue), queuedKeepAliveNanos, TimeUnit.NANOSECONDS);
                onCreate(queue);
                logger.debug("queue created...key={}", key);
            } else {
                queue = prev;
            }
        } else {
            queue.recordAccess();
        }
        return queue;
    }

    void onCreate(AbstractSizeBoundedQueue queue) {
        if (createCallback != null) {
            createCallback.callback(queue);
        }
    }

    public void onCreate(CreateCallback callback) {
        createCallback = callback;
    }

    public void clear() {
        keyToQueue.clear();
    }

    /**
     * Get all queues
     */
    public Collection<AbstractSizeBoundedQueue> elements() {
        return new ArrayList<>(keyToQueue.values());
    }

    private final class CleanTask implements TimerTask {
        private final AbstractSizeBoundedQueue queue;

        CleanTask(AbstractSizeBoundedQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run(Timeout timeout) {
            long curr = System.nanoTime();
            long deadline;

            if (queue.isEmpty() &&
                    (deadline = queue.getLastAccessNanos() + queuedKeepAliveNanos) < curr && deadline > 0) {
                Message.MessageKey key = queue.key;

                metrics.removeFromQueuedMessages(key);
                if (timeDriven.isTimerActive(key)) {
                    timeDriven.cancelTimer(key);
                }
                keyToQueue.remove(key);
            } else {
                long delay = Math.max(0L, queuedKeepAliveNanos - (curr - queue.getLastAccessNanos()));
                timeout.timer().newTimeout(this, delay, TimeUnit.NANOSECONDS);
            }
        }
    }

}
