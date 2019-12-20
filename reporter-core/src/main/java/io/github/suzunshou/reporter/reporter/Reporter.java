package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.queue.OverflowStrategy;
import io.github.suzunshou.reporter.concurrent.MessageFuture;

import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/16 4:32 下午.
 */
public interface Reporter<M extends Message, R> extends Component {

    MessageFuture<R> report(M message);

    abstract class Builder<M extends Message, R> {
        final Sender<M, R> sender;
        ReporterMetrics metrics = ReporterMetrics.NOOP_METRICS;
        OverflowStrategy.Type overflowStrategy = OverflowStrategy.Type.DropHead;
        long messageTimeoutNanos = TimeUnit.SECONDS.toNanos(1);
        int bufferedMaxMessages = 100;
        int queuedMaxMessages = 10000;

        public Builder(Sender<M, R> sender) {
            if (sender == null) {
                throw new NullPointerException("sender");
            }
            this.sender = sender;
        }

        public Builder<M, R> metrics(ReporterMetrics metrics) {
            if (metrics == null) {
                throw new NullPointerException("metrics");
            }
            this.metrics = metrics;
            return this;
        }

        public Builder<M, R> overflowStrategy(OverflowStrategy.Type overflowStrategy) {
            this.overflowStrategy = overflowStrategy;
            return this;
        }

        public Builder messageTimeout(long timeout, TimeUnit unit) {
            if (timeout < 0) throw new IllegalArgumentException("messageTimeout < 0: " + timeout);
            if (unit == null) throw new NullPointerException("unit == null");
            this.messageTimeoutNanos = unit.toNanos(timeout);
            return this;
        }

        public Builder bufferedMaxMessages(int bufferedMaxMessages) {
            if (bufferedMaxMessages < 0)
                throw new IllegalArgumentException("bufferedMaxMessages < 0: " + bufferedMaxMessages);
            this.bufferedMaxMessages = bufferedMaxMessages;
            return this;
        }

        public Builder queuedMaxMessages(int queuedMaxMessages) {
            if (queuedMaxMessages < 0)
                throw new IllegalArgumentException("queuedMaxMessages < 0: " + queuedMaxMessages);
            this.queuedMaxMessages = queuedMaxMessages;
            return this;
        }

        public abstract Reporter<M, R> build();
    }

}
