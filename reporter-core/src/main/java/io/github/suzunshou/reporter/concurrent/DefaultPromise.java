package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author zunshou on 2019/11/16 11:35 上午.
 */
public class DefaultPromise<V> extends AbstractFuture<V> implements Promise<V> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPromise.class);
    private static final Logger rejectedExecutionLogger =
            LoggerFactory.getLogger(DefaultPromise.class.getName() + ".rejectedExecution");

    private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> RESULT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");

    private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(new CancellationException());
    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    static {
        CANCELLATION_CAUSE_HOLDER.cause.setStackTrace(EMPTY_STACK_TRACE);
    }

    enum RESULT {
        SUCCESS, UNCANCELLABLE
    }

    private volatile Object result;
    private final Executor executor;

    /**
     * One or more listeners. Can be a {@link GenericFutureListener} or a {@link DefaultFutureListeners}.
     * If {@code null}, it means either 1) no listeners were added yet or 2) all listeners were notified.
     */
    private Object listeners;

    private boolean isInEventLoop;

    private short waiters;

    private boolean hasWaiters() {
        return waiters > 0;
    }

    protected Executor executor() {
        return executor;
    }

    /**
     * Creates a new instance.
     */
    public DefaultPromise(Executor executor) {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        this.executor = executor;
    }


    protected DefaultPromise() {
        // only for subclasses
        executor = ThreadPool.DIRECT_EXECUTOR_SERVICE;
    }

    public Promise<V> setSuccess(V result) {
        if (setSuccess0(result)) {
            notifyListeners();
            return this;
        }
        throw new IllegalStateException("complete already: " + this);
    }

    private void notifyListeners() {

        Executor executor = executor();

        Object listeners = null;

        synchronized (this) {
            if (isInEventLoop || this.listeners == null) {
                return;
            }
            isInEventLoop = true;
            listeners = this.listeners;
            this.listeners = null;
        }

        if (listeners instanceof DefaultFutureListeners) {
            final DefaultFutureListeners dfl = (DefaultFutureListeners) listeners;
            execute(executor, new Runnable() {
                @Override
                public void run() {
                    notifyListeners0(DefaultPromise.this, dfl);
                    DefaultPromise.this.listeners = null;
                }
            });
        } else {
            final GenericFutureListener<? extends Future<V>> l =
                    (GenericFutureListener<? extends Future<V>>) listeners;
            execute(executor, new Runnable() {
                @Override
                public void run() {
                    notifyListener0(DefaultPromise.this, l);
                    DefaultPromise.this.listeners = null;
                }
            });
        }
    }

    protected static void notifyListener(Executor executor, final Future<?> future, final GenericFutureListener<?> listener) {
        execute(executor, new Runnable() {
            @Override
            public void run() {
                notifyListener0(future, listener);
            }
        });
    }

    private static void notifyListeners0(Future<?> future, DefaultFutureListeners listeners) {
        final GenericFutureListener<?>[] a = listeners.listeners();
        final int size = listeners.size();
        for (int i = 0; i < size; i++) {
            notifyListener0(future, a[i]);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void notifyListener0(Future future, GenericFutureListener l) {
        try {
            l.operationComplete(future);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("An exception was thrown by " + l.getClass().getName() + ".operationComplete()", t);
            }
        }
    }

    private static void execute(Executor executor, Runnable task) {
        try {
            executor.execute(task);
        } catch (Throwable t) {
            rejectedExecutionLogger.error("Failed to submit a listener notification task. Event loop shut down?", t);
        }
    }

    private boolean setSuccess0(V result) {
        return setResult(result == null ? RESULT.SUCCESS : result);
    }

    private boolean setResult(Object result) {
        if (RESULT_UPDATER.compareAndSet(this, null, result)
                || RESULT_UPDATER.compareAndSet(this, RESULT.UNCANCELLABLE, result)) {
            if (hasWaiters()) {
                notifyAll();
            }
            return true;
        }
        return false;
    }


    public boolean trySuccess(V result) {
        if (setSuccess0(result)) {
            notifyListeners();
            return true;
        }
        return false;
    }

    public Promise<V> setFailure(Throwable cause) {
        if (setFailure0(cause)) {
            notifyListeners();
            return this;
        }
        throw new IllegalStateException("complete already: " + this, cause);
    }

    private boolean setFailure0(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        return setResult(new CauseHolder(cause));
    }

    public boolean tryFailure(Throwable cause) {
        if (setFailure0(cause)) {
            notifyListeners();
            return true;
        }
        return false;
    }

    public boolean setUncancellable() {
        if (RESULT_UPDATER.compareAndSet(this, null, RESULT.UNCANCELLABLE)) {
            return true;
        }
        return !isDone0(result) || !isCancelled0(result);
    }

    public Promise<V> addListener(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        if (isDone()) {
            notifyListeners();
            return this;
        }

        synchronized (this) {
            if (!isDone()) {
                if (listeners == null) {
                    listeners = listener;
                } else {
                    if (listeners instanceof DefaultFutureListeners) {
                        ((DefaultFutureListeners) listeners).add(listener);
                    } else {
                        final GenericFutureListener<? extends Future<V>> firstListener =
                                (GenericFutureListener<? extends Future<V>>) listeners;
                        listeners = new DefaultFutureListeners(firstListener, listener);
                    }
                }
                return this;
            }
        }

        notifyListener0(DefaultPromise.this, listener);

        return this;
    }

    public Promise<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        if (listeners == null) {
            throw new NullPointerException("listeners");
        }

        for (GenericFutureListener<? extends Future<? super V>> l : listeners) {
            if (l == null) {
                break;
            }
            addListener(l);
        }
        return this;
    }

    public Promise<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        if (isDone()) {
            return this;
        }

        synchronized (this) {
            if (!isDone()) {
                if (listeners instanceof DefaultFutureListeners) {
                    ((DefaultFutureListeners) listeners).remove(listener);
                } else if (listeners == listener) {
                    listeners = null;
                }
            }
        }

        return this;
    }

    public Promise<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners) {
        if (listeners == null) {
            throw new NullPointerException("listeners");
        }

        for (GenericFutureListener<? extends Future<? super V>> l : listeners) {
            if (l == null) {
                break;
            }
            removeListener(l);
        }
        return this;
    }

    public Promise<V> await() throws InterruptedException {
        if (isDone()) {
            return this;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    private void incWaiters() {
        waiters++;
    }

    private void decWaiters() {
        waiters--;
    }

    public Promise<V> awaitUninterruptibly() {
        if (isDone()) {
            return this;
        }

        boolean interrupted = false;
        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Interrupted while waiting.
                    interrupted = true;
                } finally {
                    decWaiters();
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return this;
    }

    public Promise<V> sync() throws InterruptedException {
        await();
        rethrowIfFailed();
        return this;
    }

    private void rethrowIfFailed() {
        Throwable cause = cause();
        if (cause == null) {
            return;
        }

        throw (RuntimeException) cause;
    }

    public Promise<V> syncUninterruptibly() {
        awaitUninterruptibly();
        rethrowIfFailed();
        return this;
    }

    public boolean isSuccess() {
        Object result = this.result;
        if (result == null || result == RESULT.UNCANCELLABLE) {
            return false;
        }
        return !(result instanceof CauseHolder);
    }

    public boolean isCancellable() {
        return result == null;
    }

    public Throwable cause() {
        Object result = this.result;
        if (result instanceof CauseHolder) {
            return ((CauseHolder) result).cause;
        }
        return null;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(MILLISECONDS.toNanos(timeoutMillis), true);
    }

    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toNanos(timeout), false);
        } catch (InterruptedException e) {
            // Should not be raised at all.
            throw new InternalError();
        }
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(MILLISECONDS.toNanos(timeoutMillis), false);
        } catch (InterruptedException e) {
            // Should not be raised at all.
            throw new InternalError();
        }
    }

    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }

        if (timeoutNanos <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;
        try {
            for (; ; ) {
                synchronized (this) {
                    if (isDone()) {
                        return true;
                    }
                    incWaiters();
                    try {
                        wait(waitTime / 1000000, (int) (waitTime % 1000000));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        } else {
                            interrupted = true;
                        }
                    } finally {
                        decWaiters();
                    }
                }
                if (isDone()) {
                    return true;
                } else {
                    waitTime = timeoutNanos - (System.nanoTime() - startTime);
                    if (waitTime <= 0) {
                        return isDone();
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public V getNow() {
        Object result = this.result;
        if (result instanceof CauseHolder || result == RESULT.SUCCESS) {
            return null;
        }
        return (V) result;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (RESULT_UPDATER.compareAndSet(this, null, CANCELLATION_CAUSE_HOLDER)) {
            if (hasWaiters()) {
                notifyAll();
            }
            notifyListeners();
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return isCancelled0(result);
    }

    public boolean isDone() {
        return isDone0(result);
    }

    private static final class CauseHolder {
        final Throwable cause;

        CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    private static boolean isCancelled0(Object result) {
        return result instanceof CauseHolder && ((CauseHolder) result).cause instanceof CancellationException;
    }

    private static boolean isDone0(Object result) {
        return result != null && result != RESULT.UNCANCELLABLE;
    }
}
