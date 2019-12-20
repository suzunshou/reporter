package io.github.suzunshou.reporter.queue;

/**
 * @author zunshou on 2019/11/18 2:25 下午.
 */
public interface CreateCallback {

    void callback(AbstractSizeBoundedQueue queue);
}
