package io.github.suzunshou.reporter.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/19 10:14 上午.
 */
public class LogReporterMetricsExporter extends ReporterMetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LogReporterMetricsExporter.class);
    private ScheduledExecutorService executor;

    @Override
    public void start(final ReporterMetrics metrics) {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                logger.debug("Messages: {}\tMessagesDropped: {}\tQueuedMessages: {}",
                        metrics.messages(), metrics.messagesDropped(), metrics.queuedMessages());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
        }
    }
}
