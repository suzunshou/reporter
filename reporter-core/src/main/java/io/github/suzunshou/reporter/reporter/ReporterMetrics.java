package io.github.suzunshou.reporter.reporter;

/**
 * Instrumented applications report.
 *
 * @author zunshou on 2019/11/17 11:48 上午.
 */
public interface ReporterMetrics {

    enum MetricKey {
        messages,
        messageDropped
    }

    /**
     * Increments count of message attempts, which contain 1 or more spans. Ex POST requests or Kafka
     * messages sent.
     */
    void incrementMessages(int quantity);

    /**
     * Increments count of messages that could not be sent. Ex host unavailable, or peer disconnect.
     */
    void incrementMessagesDropped(int quantity);

    long messages();

    long messagesDropped();

    long queuedMessages();

    void updateQueuedMessages(Message.MessageKey key, int update);

    void removeFromQueuedMessages(Message.MessageKey key);

    ReporterMetrics NOOP_METRICS = new ReporterMetrics() {

        @Override
        public void incrementMessages(int quantity) {

        }

        @Override
        public void incrementMessagesDropped(int quantity) {

        }

        @Override
        public long messages() {
            return 0;
        }

        @Override
        public long messagesDropped() {
            return 0;
        }

        @Override
        public long queuedMessages() {
            return 0;
        }

        @Override
        public void updateQueuedMessages(Message.MessageKey key, int quantity) {

        }

        @Override
        public void removeFromQueuedMessages(Message.MessageKey key) {

        }

        @Override
        public String toString() {
            return "NoOpReporterMetrics";
        }
    };
}
