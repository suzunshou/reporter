package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.buffer.OverflowStrategy;
import io.github.suzunshou.reporter.reporter.Message;

/**
 * @author zunshou on 2019/11/17 6:18 下午.
 */
public final class ConcurrentSizeBoundedQueueFactory extends SizeBoundedQueueFactory {

    @Override
    public AbstractSizeBoundedQueue newSizeBoundedQueue(int maxSize, OverflowStrategy.Type overflowStrategy, Message.MessageKey key) {
        return new ConcurrentSizeBoundedQueue(maxSize, key, overflowStrategy);
    }
}
