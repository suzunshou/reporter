package io.github.suzunshou.reporter.threadpool;

import java.util.concurrent.*;

/**
 * @author zunshou on 2019/11/17 9:46 上午.
 */
public class ThreadPool {

    //direct execute command
    public static final ExecutorService DIRECT_EXECUTOR_SERVICE = ReporterExecutors.newDirectExecutorService();

    //for sender
    private static final ThreadFactory ASYNC_SENDER_THREAD_FACTORY = ReporterExecutors.daemonThreadFactory("Async-Sender-");


    public static class ExecutorHolder {
        private Executor executor;
        private final int nThread;
        private int refCnt = 1;

        public ExecutorHolder(int nThread) {
            this.nThread = nThread;
        }

        public synchronized Executor executor() {
            if (executor == null) {
                executor = new ThreadPoolExecutor(nThread, nThread,
                        0, TimeUnit.MILLISECONDS,
                        new SynchronousQueue<Runnable>(), ASYNC_SENDER_THREAD_FACTORY,
                        new ThreadPoolExecutor.CallerRunsPolicy());
            } else {
                increment();
            }
            return executor;
        }

        synchronized void increment() {
            refCnt++;
        }

        synchronized int getRefCnt() {
            return refCnt;
        }

        public synchronized boolean close() {
            if (--refCnt == 0 && executor != null) {
                ExecutorService executorService = (ExecutorService) executor;
                executorService.shutdown();
                try {
                    executorService.awaitTermination(500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
            return false;
        }
    }
}
