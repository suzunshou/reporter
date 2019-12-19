package io.github.suzunshou.reporter.concurrent;

import java.util.EventListener;

/**
 * @author zunshou on 2019/11/16 11:32 上午.
 * Listens to the result of a {@link Future}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(GenericFutureListener)}.
 */
public interface GenericFutureListener<F extends Future<?>> extends EventListener {
    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * @param future the source {@link Future} which called this callback
     */
    void operationComplete(F future) throws Exception;
}
