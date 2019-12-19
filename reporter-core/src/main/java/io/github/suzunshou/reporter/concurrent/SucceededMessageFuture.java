package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 6:06 下午.
 * The {@link CompleteMessageFuture} which is succeeded already.
 */
final class SucceededMessageFuture extends CompleteMessageFuture {

    /**
     * Creates a new instance.
     *
     * @param message the {@link Message} associated with this future
     */
    SucceededMessageFuture(Message message, Executor executor) {
        super(message, executor);
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public Throwable cause() {
        return null;
    }
}
