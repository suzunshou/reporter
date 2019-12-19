package io.github.suzunshou.reporter.transfer;

import io.github.suzunshou.reporter.concurrent.DefaultMessagePromise;
import io.github.suzunshou.reporter.concurrent.Future;
import io.github.suzunshou.reporter.concurrent.GenericFutureListener;
import io.github.suzunshou.reporter.concurrent.MessageFuture;

/**
 * @author zunshou on 2019/12/16 3:00 下午.
 */
public class TransferUtil {

    public static <T> MessageFuture<T> buildResponse(MessageFuture messageFuture, final Transfer<T> transfer) {
        final DefaultMessagePromise<T> promise = new DefaultMessagePromise<>(messageFuture.message());
        messageFuture.addListener(new GenericFutureListener<Future<?>>() {
            @Override
            public void operationComplete(Future<?> future) throws Exception {
                if (future.isSuccess()) {
                    promise.setSuccess(transfer.trans(future.get()));
                } else {
                    promise.setFailure(future.cause());
                }
            }
        });
        return promise;
    }
}
