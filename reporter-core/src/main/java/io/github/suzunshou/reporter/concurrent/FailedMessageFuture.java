package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 5:58 下午.
 * The {@link CompleteMessageFuture} which is failed already.
 */
public class FailedMessageFuture extends CompleteMessageFuture {

    private final Throwable cause;

    /**
     * Creates a new instance.
     *
     * @param message the {@link Message} associated with this future
     * @param cause   the cause of failure
     */
    public FailedMessageFuture(Message message, Executor executor, Throwable cause) {
        super(message, executor);
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        this.cause = cause;
    }

    @Override
    public Throwable cause() {
        return cause;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public MessageFuture sync() {
        throw (RuntimeException) cause;
    }

    @Override
    public MessageFuture syncUninterruptibly() {
        throw (RuntimeException) cause;
    }
}
