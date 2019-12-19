package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 5:10 下午.
 */
public class DefaultMessagePromise<V> extends DefaultPromise<V> implements MessagePromise<V> {

    private final Message message;

    public DefaultMessagePromise(Message message) {
        this.message = message;
    }

    public DefaultMessagePromise(Executor executor, Message message) {
        super(executor);
        this.message = message;
    }

    @Override
    protected Executor executor() {
        return super.executor();
    }

    @Override
    public Message message() {
        return message;
    }

    @Override
    public MessagePromise<V> setSuccess() {
        return setSuccess(null);
    }

    @Override
    public MessagePromise<V> setSuccess(V result) {
        super.setSuccess(result);
        return this;
    }

    @Override
    public boolean trySuccess() {
        return trySuccess(null);
    }

    @Override
    public MessagePromise<V> setFailure(Throwable cause) {
        super.setFailure(cause);
        return this;
    }

    @Override
    public MessagePromise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public MessagePromise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public MessagePromise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public MessagePromise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public MessagePromise<V> sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public MessagePromise<V> syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public MessagePromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public MessagePromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }
}
