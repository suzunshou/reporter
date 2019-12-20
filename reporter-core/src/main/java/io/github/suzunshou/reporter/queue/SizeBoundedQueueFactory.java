package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.reporter.Message;
import io.github.suzunshou.reporter.util.ExtensionLoader;

import java.util.ServiceConfigurationError;

/**
 * @author zunshou on 2019/11/17 6:12 下午.
 */
public abstract class SizeBoundedQueueFactory {
    //load factory
    private static final SizeBoundedQueueFactory factory = ExtensionLoader.getExtension(SizeBoundedQueueFactory.class);

    public abstract AbstractSizeBoundedQueue newSizeBoundedQueue(int maxSize, OverflowStrategy.Type overflowStrategy, Message.MessageKey key);

    static SizeBoundedQueueFactory factory() {
        if (factory == null) {
            throw new ServiceConfigurationError("No functional queue factory found.");
        }
        return factory;
    }
}
