package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.buffer.OverflowStrategy;

import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/19 9:54 上午.
 */
public class AsyncReporterProperties extends AbstractReporterProperties {

    static final int DEFAULT_TIMER_THREADS = 1;
    static final int DEFAULT_FLUSH_THREADS = 1;

    private int senderThreads = 1;
    private long queuedKeepaliveNanos = TimeUnit.SECONDS.toNanos(60);
    private long tickDurationNanos = TimeUnit.MILLISECONDS.toNanos(100);
    private int ticksPerWheel = 512;
    private int timerThreads = DEFAULT_TIMER_THREADS;
    private int flushThreads = DEFAULT_FLUSH_THREADS;
    private int totalQueuedMessages = 100000;

    @Override
    public AsyncReporterProperties setMetrics(String metrics) {
        super.setMetrics(metrics);
        return this;
    }

    @Override
    public AsyncReporterProperties setMessageTimeoutNanos(long messageTimeout, TimeUnit unit) {
        super.setMessageTimeoutNanos(messageTimeout, unit);
        return this;
    }

    @Override
    public AsyncReporterProperties setBufferedMaxMessages(int bufferedMaxMessages) {
        super.setBufferedMaxMessages(bufferedMaxMessages);
        return this;
    }

    @Override
    public AsyncReporterProperties setQueuedMaxMessages(int queuedMaxMessages) {
        super.setQueuedMaxMessages(queuedMaxMessages);
        return this;
    }


    @Override
    public AsyncReporterProperties setMetricsExporter(String metricsExporter) {
        super.setMetricsExporter(metricsExporter);
        return this;
    }

    @Override
    public AsyncReporterProperties setOverflowStrategy(OverflowStrategy.Type overflowStrategy) {
        super.setOverflowStrategy(overflowStrategy);
        return this;
    }

    public int getSenderThreads() {
        return senderThreads;
    }

    public AsyncReporterProperties setSenderThreads(int senderThreads) {
        this.senderThreads = senderThreads;
        return this;
    }

    public long getQueuedKeepaliveNanos() {
        return queuedKeepaliveNanos;
    }

    public AsyncReporterProperties setQueuedKeepaliveNanos(long queuedKeepaliveNanos) {
        this.queuedKeepaliveNanos = queuedKeepaliveNanos;
        return this;
    }

    public long getTickDurationNanos() {
        return tickDurationNanos;
    }

    public AsyncReporterProperties setTickDurationNanos(long tickDurationNanos) {
        this.tickDurationNanos = tickDurationNanos;
        return this;
    }

    public int getTicksPerWheel() {
        return ticksPerWheel;
    }

    public AsyncReporterProperties setTicksPerWheel(int ticksPerWheel) {
        this.ticksPerWheel = ticksPerWheel;
        return this;
    }

    public int getFlushThreads() {
        return flushThreads;
    }

    public AsyncReporterProperties setFlushThreads(int flushThreads) {
        this.flushThreads = flushThreads;
        return this;
    }

    public int getTimerThreads() {
        return timerThreads;
    }

    public AsyncReporterProperties setTimerThreads(int timerThreads) {
        this.timerThreads = timerThreads;
        return this;
    }

    public int getTotalQueuedMessages() {
        return totalQueuedMessages;
    }

    public AsyncReporterProperties setTotalQueuedMessages(int totalQueuedMessages) {
        this.totalQueuedMessages = totalQueuedMessages;
        return this;
    }

    @Override
    public <M extends Message, R> AsyncReporter.Builder<M, R> toBuilder(Sender<M, R> sender) {
        AsyncReporter.Builder builder = new AsyncReporter.Builder<>(sender).nThreads(senderThreads)
                .messageTimeout(messageTimeoutNanos, TimeUnit.NANOSECONDS)
                .queuedKeepAliveNanos(queuedKeepaliveNanos, TimeUnit.NANOSECONDS)
                .tickDuration(tickDurationNanos, TimeUnit.NANOSECONDS)
                .ticksPerWheel(ticksPerWheel)
                .flushThreads(flushThreads)
                .timerThreads(timerThreads)
                .bufferedMaxMessages(bufferedMaxMessages)
                .queuedMaxMessages(queuedMaxMessages)
                .overflowStrategy(overflowStrategy)
                .totalQueuedMessages(totalQueuedMessages);

        if (metrics.equalsIgnoreCase("inmemory")) {
            builder.metrics(InMemoryReporterMetrics.instance(ReporterMetricsExporter.of(metricsExporter)));
        }

        return builder;
    }
}
