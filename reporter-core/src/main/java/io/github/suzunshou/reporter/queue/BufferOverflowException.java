package io.github.suzunshou.reporter.queue;

/**
 * @author zunshou on 2019/11/18 11:22 上午.
 */
class BufferOverflowException extends RuntimeException {

    BufferOverflowException(String message) {
        super(message);
    }
}
