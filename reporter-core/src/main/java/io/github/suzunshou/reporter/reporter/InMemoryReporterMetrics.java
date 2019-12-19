package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.concurrent.chmv8.LongAdder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zunshou on 2019/11/17 12:03 下午.
 */
public final class InMemoryReporterMetrics implements ReporterMetrics {

    public void startExporter(final ReporterMetricsExporter exporter) {
        exporter.start(this);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                exporter.close();
            }
        });
    }

    private InMemoryReporterMetrics(ReporterMetricsExporter exporter) {
        startExporter(exporter);
    }

    enum MetricKey {
        messages,
        messageDropped
    }

    private final ConcurrentHashMap<MetricKey, AtomicLong> metrics = new ConcurrentHashMap<MetricKey, AtomicLong>();

    private final ConcurrentHashMap<Message.MessageKey, AtomicLong> queuedMessages = new ConcurrentHashMap<Message.MessageKey, AtomicLong>();

    private final LongAdder queuedMessagesAccumulator = new LongAdder();

    @Override
    public void incrementMessages(int quantity) {
        increment(metrics, MetricKey.messages, quantity);
    }

    @Override
    public void incrementMessagesDropped(int quantity) {
        increment(metrics, MetricKey.messageDropped, quantity);
    }

    @Override
    public long messages() {
        return get(MetricKey.messages);
    }

    @Override
    public long messagesDropped() {
        return get(MetricKey.messageDropped);
    }

    @Override
    public long queuedMessages() {
        return queuedMessagesAccumulator.sum();
    }

    @Override
    public void updateQueuedMessages(Message.MessageKey key, int update) {
        AtomicLong metric = queuedMessages.get(key);
        if (metric == null) {
            metric = queuedMessages.putIfAbsent(key, new AtomicLong(update));
            if (metric == null) {
                queuedMessagesAccumulator.add(update);
                return;
            }
        }
        long prev = metric.getAndSet(update);
        queuedMessagesAccumulator.add(update - prev);
    }

    @Override
    public void removeFromQueuedMessages(Message.MessageKey key) {
        AtomicLong value = queuedMessages.remove(key);
        if (value != null) {
            queuedMessagesAccumulator.add(-value.get());
        }
    }

    static <K> void increment(ConcurrentHashMap<K, AtomicLong> metrics, K key, int quantity) {
        if (quantity == 0) return;
        while (true) {
            AtomicLong metric = metrics.get(key);
            if (metric == null) {
                metric = metrics.putIfAbsent(key, new AtomicLong(quantity));
                if (metric == null) return; // won race creating the entry
            }

            while (true) {
                long oldValue = metric.get();
                long update = oldValue + quantity;
                if (metric.compareAndSet(oldValue, update)) return; // won race updating
            }
        }
    }

    private long get(MetricKey key) {
        AtomicLong atomic = metrics.get(key);
        return atomic == null ? 0 : atomic.get();
    }

    private static InMemoryReporterMetrics instance;

    public static InMemoryReporterMetrics instance(ReporterMetricsExporter exporter) {
        if (instance == null) {
            synchronized (InMemoryReporterMetrics.class) {
                if (instance == null) {
                    instance = new InMemoryReporterMetrics(exporter);
                }
            }
        }
        return instance;
    }
}
