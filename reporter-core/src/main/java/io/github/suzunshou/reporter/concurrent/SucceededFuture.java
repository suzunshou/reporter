package io.github.suzunshou.reporter.concurrent;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 6:11 下午.
 * The {@link CompleteFuture} which is succeeded already.
 */
public final class SucceededFuture<V> extends CompleteFuture<V> {

    private final V result;

    public SucceededFuture(Executor executor, V result) {
        super(executor);
        this.result = result;
    }

    @Override
    public Throwable cause() {
        return null;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public V getNow() {
        return result;
    }
}
