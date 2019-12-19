package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.concurrent.DefaultMessagePromise;
import io.github.suzunshou.reporter.concurrent.FailedMessageFuture;
import io.github.suzunshou.reporter.concurrent.MessageFuture;
import io.github.suzunshou.reporter.concurrent.MessagePromise;

import java.io.Serializable;

/**
 * @author zunshou on 2019/11/16 11:30 上午.
 */
public abstract class Message implements Serializable {

    private static final long serialVersionUID = -3090185744179783876L;

    protected abstract MessageKey asMessageKey();

    @Override
    public abstract String toString();

    <V> MessagePromise<V> newPromise() {
        return new DefaultMessagePromise<V>(this);
    }

    MessageFuture newFailedFuture(Throwable cause) {
        return new FailedMessageFuture(this, null, cause);
    }

    public static abstract class MessageKey {
        @Override
        public abstract int hashCode();

        @Override
        public abstract boolean equals(Object obj);

        @Override
        public abstract String toString();
    }
}
