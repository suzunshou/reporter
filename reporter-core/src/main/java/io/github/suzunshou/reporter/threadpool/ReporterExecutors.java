package io.github.suzunshou.reporter.threadpool;


import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zunshou on 2019/11/17 9:48 上午.
 */
public class ReporterExecutors {

    private static final ExecutorService DIRECT_EXECUTOR_SERVICE = new DirectExecutorService();

    static ExecutorService newDirectExecutorService() {
        return DIRECT_EXECUTOR_SERVICE;
    }

    //direct run command...
    private static final class DirectExecutorService extends AbstractExecutorService {

        DirectExecutorService() {
            super();
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    public static ThreadFactory daemonThreadFactory(String namePrefix) {
        return new ReporterThreadFactory(namePrefix);
    }

    static class ReporterThreadFactory implements ThreadFactory {

        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        ReporterThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + "[T#" + threadNumber.getAndIncrement() + "]", 0);
            t.setDaemon(true);
            return t;
        }

    }

}
