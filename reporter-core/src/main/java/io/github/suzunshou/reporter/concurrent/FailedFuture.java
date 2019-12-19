package io.github.suzunshou.reporter.concurrent;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 6:09 下午.
 * The {@link CompleteFuture} which is failed already.
 */
public final class FailedFuture<V> extends CompleteFuture<V> {

    private final Throwable cause;

    public FailedFuture(Executor executor, Throwable cause) {
        super(executor);
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
    public Future<V> sync() {
        throw (RuntimeException) cause;
    }

    @Override
    public Future<V> syncUninterruptibly() {
        throw (RuntimeException) cause;
    }

    @Override
    public V getNow() {
        return null;
    }
}
