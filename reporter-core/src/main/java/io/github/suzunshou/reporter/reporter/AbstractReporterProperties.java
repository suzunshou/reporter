package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.queue.OverflowStrategy;

import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/19 9:48 上午.
 */
public abstract class AbstractReporterProperties implements ReporterProperties {

    protected Sender<? extends Message, ?> sender;
    protected String metrics = "noop";
    protected String metricsExporter = "noop";
    protected long messageTimeoutNanos = TimeUnit.SECONDS.toNanos(1);
    protected int bufferedMaxMessages = 100;
    protected int queuedMaxMessages = 10000;
    protected OverflowStrategy.Type overflowStrategy = OverflowStrategy.Type.DropHead;

    public String getMetrics() {
        return metrics;
    }

    public AbstractReporterProperties setMetrics(String metrics) {
        if (!metrics.equalsIgnoreCase("inmemory")) {
            throw new UnsupportedOperationException(metrics);
        }
        this.metrics = metrics;
        return this;
    }

    public long getMessageTimeoutNanos() {
        return messageTimeoutNanos;
    }

    public AbstractReporterProperties setMessageTimeoutNanos(long messageTimeout, TimeUnit unit) {
        this.messageTimeoutNanos = unit.toNanos(messageTimeout);
        return this;
    }

    public int getBufferedMaxMessages() {
        return bufferedMaxMessages;
    }

    public AbstractReporterProperties setBufferedMaxMessages(int bufferedMaxMessages) {
        this.bufferedMaxMessages = bufferedMaxMessages;
        return this;
    }


    public int getQueuedMaxMessages() {
        return queuedMaxMessages;
    }

    public AbstractReporterProperties setQueuedMaxMessages(int queuedMaxMessages) {
        this.queuedMaxMessages = queuedMaxMessages;
        return this;
    }

    public OverflowStrategy.Type getOverflowStrategy() {
        return overflowStrategy;
    }

    public AbstractReporterProperties setOverflowStrategy(OverflowStrategy.Type overflowStrategy) {
        this.overflowStrategy = overflowStrategy;
        return this;
    }

    public String getMetricsExporter() {
        return metricsExporter;
    }

    public AbstractReporterProperties setMetricsExporter(String metricsExporter) {
        this.metricsExporter = metricsExporter;
        return this;
    }
}
