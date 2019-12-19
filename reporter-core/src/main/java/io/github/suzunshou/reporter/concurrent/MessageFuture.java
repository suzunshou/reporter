package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

/**
 * @author zunshou on 2019/11/16 5:01 下午.
 * The result of an asynchronous {@link Message} I/O operation.
 */
public interface MessageFuture<V> extends Future<V> {

    /**
     * Returns a {@link Message}  where the I/O operation associated with this
     * future takes place.
     */
    Message message();

    @Override
    MessageFuture<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    MessageFuture<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    MessageFuture<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    MessageFuture<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    MessageFuture<V> sync() throws InterruptedException;

    @Override
    MessageFuture<V> syncUninterruptibly();

    @Override
    MessageFuture<V> await() throws InterruptedException;

    @Override
    MessageFuture<V> awaitUninterruptibly();
}
