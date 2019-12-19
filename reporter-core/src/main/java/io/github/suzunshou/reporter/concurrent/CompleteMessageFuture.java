package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.reporter.Message;

import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 5:45 下午.
 * A skeletal {@link MessageFuture} implementation which represents a
 * {@link MessageFuture} which has been completed already.
 */
abstract class CompleteMessageFuture<V> extends CompleteFuture<V> implements MessageFuture<V> {

    private Message message;

    /**
     * Creates a new instance.
     *
     * @param message the {@link Message} associated with this future
     */
    public CompleteMessageFuture(Message message, Executor executor) {
        super(executor);
        if (message == null) {
            throw new NullPointerException("message");
        }
        this.message = message;
    }

    @Override
    public Executor executor() {
        return super.executor();
    }

    @Override
    public MessageFuture<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public MessageFuture<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public MessageFuture<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public MessageFuture<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public MessageFuture<V> syncUninterruptibly() {
        return this;
    }

    @Override
    public MessageFuture<V> sync() throws InterruptedException {
        return this;
    }

    @Override
    public MessageFuture<V> await() throws InterruptedException {
        return this;
    }

    @Override
    public MessageFuture<V> awaitUninterruptibly() {
        return this;
    }

    @Override
    public Message message() {
        return message;
    }

    @Override
    public V getNow() {
        return null;
    }
}
