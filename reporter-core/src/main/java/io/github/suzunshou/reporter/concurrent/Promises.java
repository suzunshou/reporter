package io.github.suzunshou.reporter.concurrent;

import io.github.suzunshou.reporter.buffer.MessageDroppedException;
import io.github.suzunshou.reporter.buffer.OverflowStrategy;
import io.github.suzunshou.reporter.reporter.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author zunshou on 2019/11/17 11:31 上午.
 */
public class Promises {

    /**
     * mark promise success
     *
     * @param result
     * @param promises
     * @param <R>
     */
    public static <R> void allSuccess(List<R> result, List<MessagePromise<R>> promises) {
        if (result.size() != promises.size()) {
            throw new IllegalArgumentException("result.size() != promises.size()");
        }
        for (int i = 0; i < promises.size(); i++) {
            MessagePromise<R> promise = promises.get(i);
            R res = result.get(i);
            if (res instanceof Throwable) {
                promise.setFailure(MessageDroppedException.dropped((Throwable) res,
                        Collections.singletonList(promise.message())));
            } else {
                promise.setSuccess(res);
            }
        }
    }

    /**
     * mark promise fail
     *
     * @param cause
     * @param promises
     * @param messages
     */
    public static void allFail(Throwable cause,
                               List<? extends MessagePromise<?>> promises, List<? extends Message> messages) {
        if (messages.size() != promises.size()) {
            throw new IllegalArgumentException("messages.size() != promises.size()");
        }

        for (MessagePromise<?> promise : promises) {
            promise.setFailure(MessageDroppedException.dropped(cause, messages));
        }
    }

    /**
     * mark promise fail
     *
     * @param promises
     * @param overflowStrategy
     */
    public static void allFail(List<MessagePromise<?>> promises, OverflowStrategy overflowStrategy) {
        List<Message> messages = new ArrayList<Message>();
        for (MessagePromise<?> promise : promises) {
            messages.add(promise.message());
        }
        for (MessagePromise<?> promise : promises) {
            promise.setFailure((MessageDroppedException.dropped(overflowStrategy, messages)));
        }
    }

}
