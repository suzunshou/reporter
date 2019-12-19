package io.github.suzunshou.reporter.reporter;

import java.io.Closeable;

/**
 * @author zunshou on 2019/11/16 4:34 下午.
 */
public interface Component extends Closeable {

    CheckResult check();

    class CheckResult {

        public static final CheckResult OK = new CheckResult(true, null);

        final boolean ok;

        final Exception exception;

        public static CheckResult failed(Exception error) {
            return new CheckResult(false, error);
        }

        CheckResult(boolean ok, Exception exception) {
            this.ok = ok;
            this.exception = exception;
        }
    }
}
