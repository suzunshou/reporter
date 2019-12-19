package io.github.suzunshou.reporter.concurrent.timer;

import java.util.concurrent.TimeUnit;

/**
 * @author zunshou on 2019/11/17 3:54 下午.
 * A task which is executed after the delay specified with
 * {@link Timer#newTimeout(TimerTask, long, TimeUnit)}.
 */
public interface TimerTask {
    /**
     * Executed after the delay specified with
     * {@link Timer#newTimeout(TimerTask, long, TimeUnit)}.
     *
     * @param timeout a handle which is associated with this task
     */
    void run(Timeout timeout) throws Exception;
}
