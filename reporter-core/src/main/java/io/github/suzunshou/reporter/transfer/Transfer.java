package io.github.suzunshou.reporter.transfer;

/**
 * @author zunshou on 2019/12/16 2:54 下午.
 */
public abstract class Transfer<T> {

    public abstract T trans(Object data);
}
