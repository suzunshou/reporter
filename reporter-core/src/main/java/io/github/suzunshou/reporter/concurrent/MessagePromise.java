package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

/**
 * @author zunshou on 2019/11/16 5:05 下午.
 * Special {@link MessageFuture} which is writable.
 */
public interface MessagePromise<V> extends MessageFuture<V>, Promise<V> {

    @Override
    Message message();

    @Override
    MessagePromise<V> setSuccess(V result);

    MessagePromise<V> setSuccess();

    boolean trySuccess();

    @Override
    MessagePromise<V> setFailure(Throwable cause);

    @Override
    MessagePromise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    MessagePromise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    MessagePromise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    @Override
    MessagePromise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    @Override
    MessagePromise<V> sync() throws InterruptedException;

    @Override
    MessagePromise<V> syncUninterruptibly();

    @Override
    MessagePromise<V> await() throws InterruptedException;

    @Override
    MessagePromise<V> awaitUninterruptibly();
}
