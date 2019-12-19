package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.concurrent.CompositeFuture;
import io.github.suzunshou.reporter.concurrent.MessagePromise;
import io.github.suzunshou.reporter.concurrent.Promises;
import io.github.suzunshou.reporter.threadpool.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author zunshou on 2019/11/16 8:04 下午.
 */
public final class DefaultAsyncSender<M extends Message, R> implements AsyncSender<R> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncSender.class);
    private final Sender<M, R> sender;
    private Executor executor;
    static ThreadPool.ExecutorHolder executorHolder;

    public DefaultAsyncSender(Sender<M, R> sender, int nThreads) {
        if (sender == null) {
            throw new NullPointerException("sender");
        }

        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads must be greater than zero");
        }
        this.sender = sender;
        synchronized (DefaultAsyncSender.class) {
            if (executorHolder == null) {
                executorHolder = new ThreadPool.ExecutorHolder(nThreads);
            }
        }
        this.executor = executorHolder.executor();
    }

    @Override
    public CompositeFuture send(final List<MessagePromise<R>> promises) {
        logger.debug("send | message count = {}", promises.size());
        final List<M> messages = new ArrayList<M>();
        for (MessagePromise promise : promises) {
            messages.add((M) promise.message());
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<R> result = sender.send(messages);
                    Promises.allSuccess(result, promises);
                } catch (Exception e) {
                    Promises.allFail(e, promises, messages);
                }
            }
        });
        return CompositeFuture.all(promises);
    }


    @Override
    public Component.CheckResult check() {
        return sender.check();
    }

    @Override
    public void close() throws IOException {
        synchronized (DefaultAsyncSender.class) {
            if (executorHolder != null && executorHolder.close()) {
                executorHolder = null;
            }
        }
        sender.close();
    }
}
