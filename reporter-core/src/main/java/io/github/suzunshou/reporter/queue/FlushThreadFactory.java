package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.reporter.AsyncReporter;
import io.github.suzunshou.reporter.threadpool.ReporterExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

/**
 * @author zunshou on 2019/11/18 10:51 下午.
 */
public class FlushThreadFactory {
    private static final Logger logger = LoggerFactory.getLogger(FlushThreadFactory.class);
    private final AsyncReporter<?, ?> reporter;
    private final FlushSynchronizer synchronizer;
    private final ThreadFactory threadFactory;

    public FlushThreadFactory(AsyncReporter<?, ?> reporter) {
        this.reporter = reporter;
        this.synchronizer = reporter.flushSynchronizer;
        this.threadFactory = ReporterExecutors.daemonThreadFactory("FlushThreadFactory-" + reporter.id + "-flusher-");
    }

    public Thread newFlushThread() {
        return threadFactory.newThread(new FlushRunnable());
    }

    private void scheduleAndFlush(AbstractSizeBoundedQueue queue) {
        try {
            while (queue.size() >= reporter.queuedMaxMessages) {
                if (reporter.scheduler != null) {
                    reporter.schedulePeriodically(queue.key, reporter.messageTimeoutNanos);
                }
                reporter.flush(queue);
            }
        } finally {
            synchronizer.release(queue);
        }
    }

    private class FlushRunnable implements Runnable {
        @Override
        public void run() {
            try {
                while (AsyncReporter.REPORTER_STATE_UPDATER.get(reporter) != AsyncReporter.REPORTER_STATE_SHUTDOWN) {
                    AbstractSizeBoundedQueue queue = synchronizer.poll(reporter.messageTimeoutNanos);
                    if (Thread.interrupted()) {
                        logger.warn("Interrupted while waiting for a ready queue");
                        break;
                    }
                    if (queue == null) {
                        continue;
                    }
                    scheduleAndFlush(queue);
                }
            } finally {
                if (AsyncReporter.REPORTER_STATE_UPDATER.get(reporter) == AsyncReporter.REPORTER_STATE_SHUTDOWN) {
                    reporter.close.countDown();
                }
            }
        }

    }
}
