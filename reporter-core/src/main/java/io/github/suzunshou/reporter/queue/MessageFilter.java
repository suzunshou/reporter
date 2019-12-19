package io.github.suzunshou.reporter.queue;


import io.github.suzunshou.reporter.concurrent.MessagePromise;

/**
 * @author zunshou on 2019/11/17 5:32 下午.
 */
public interface MessageFilter {

    boolean accept(MessagePromise<?> promise);
}
