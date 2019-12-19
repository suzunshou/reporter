package io.github.suzunshou.reporter.concurrent;

import java.util.List;

/**
 * @author zunshou on 2019/11/16 6:57 下午.
 * The composite future wraps a list of {@link Future futures}, it is useful when several futures
 * needs to be coordinated.
 * The handlers set for the coordinated futures are overridden by the handler of the composite future.
 */
public abstract class CompositeFuture extends AbstractFuture<CompositeFuture> {

    /**
     * When the list is empty, the returned future will be already completed.
     */
    public static CompositeFuture all(List<? extends Future<?>> futures) {
        return DefaultCompositeFuture.all(futures.toArray(new Future[futures.size()]));
    }

    /**
     * Returns a cause of a wrapped future
     *
     * @param index the wrapped future index
     */
    public abstract Throwable cause(int index);


    /**
     * Returns true if a wrapped future is completed
     *
     * @param index the wrapped future index
     */
    public abstract boolean isComplete(int index);

    /**
     * Returns true if a wrapped future is succeeded
     *
     * @param index the wrapped future index
     */
    public abstract boolean succeeded(int index);

    /**
     * Returns the result of a wrapped future
     *
     * @param index the wrapped future index
     */
    public abstract <T> T resultAt(int index);


    @Override
    public abstract DefaultCompositeFuture addListener(GenericFutureListener<? extends Future<? super CompositeFuture>> listener);
}
