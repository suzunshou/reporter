package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.buffers.Buffer;
import io.github.suzunshou.buffers.BufferPool;
import io.github.suzunshou.reporter.buffer.MessageDroppedException;
import io.github.suzunshou.reporter.buffer.OverflowStrategy;
import io.github.suzunshou.reporter.concurrent.Future;
import io.github.suzunshou.reporter.concurrent.*;
import io.github.suzunshou.reporter.concurrent.timer.HashedWheelTimer;
import io.github.suzunshou.reporter.concurrent.timer.TimeDriven;
import io.github.suzunshou.reporter.queue.*;
import io.github.suzunshou.reporter.threadpool.ReporterExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Flushable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zunshou on 2019/11/17 11:49 上午.
 */
public class AsyncReporter<M extends Message, R> extends TimeDriven<Message.MessageKey> implements Reporter<M, R>, Flushable {
    private static final Logger logger = LoggerFactory.getLogger(AsyncReporter.class);

    static final int MAX_BUFFER_POOL_ENTRIES = 1000;
    private final AtomicLong idGen = new AtomicLong();
    public final Long id = idGen.getAndIncrement();
    final AsyncSender<R> sender;
    public final long messageTimeoutNanos;
    public final int queuedMaxMessages;
    final ReporterMetrics metrics;

    final int timerThreads;
    private final int flushThreads;
    Set<Thread> flushers;
    private final FlushThreadFactory flushThreadFactory;

    private static HashedWheelTimer hashedWheelTimer;
    private static ThreadFactory hashedWheelTimerThreadFactory;

    public final FlushSynchronizer flushSynchronizer = new FlushSynchronizer();
    final QueueManager queueManager;

    public static final int REPORTER_STATE_INIT = 0;
    public static final int REPORTER_STATE_STARTED = 1;
    public static final int REPORTER_STATE_SHUTDOWN = 2;

    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int reporterState; // 0 - init, 1 - started, 2 - shut down

    public static final AtomicIntegerFieldUpdater<AsyncReporter> REPORTER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AsyncReporter.class, "reporterState");

    public CountDownLatch close;

    public ScheduledExecutorService scheduler;

    private BufferPool bufferPool;
    private final MemoryLimiter memoryLimiter;

    static {
        hashedWheelTimerThreadFactory =
                ReporterExecutors.daemonThreadFactory(AsyncReporter.class.getSimpleName() + "-cleaner-");
    }

    AsyncReporter(Builder<M, R> builder) {
        this.sender = new DefaultAsyncSender<>(builder.sender, builder.nThreads);
        this.metrics = builder.metrics;
        this.memoryLimiter = MemoryLimiter.maxOf(builder.bufferedMaxMessages, metrics);
        this.messageTimeoutNanos = builder.messageTimeoutNanos;
        this.queuedMaxMessages = builder.queuedMaxMessages;
        this.flushThreads = builder.flushThreads;
        this.timerThreads = builder.timerThreads;
        this.queueManager = new QueueManager(builder.queuedMaxMessages, builder.overflowStrategy,
                builder.queuedKeepAliveNanos, this, metrics, initHashedWheelTimer(builder));

        this.flushThreadFactory = new FlushThreadFactory(this);
        this.bufferPool = new BufferPool(MAX_BUFFER_POOL_ENTRIES, builder.bufferedMaxMessages);

        if (messageTimeoutNanos > 0) {
            this.queueManager.onCreate(new CreateCallback() {
                @Override
                public void call(AbstractSizeBoundedQueue queue) {
                    schedulePeriodically(queue.key, messageTimeoutNanos);
                }
            });
        }
    }

    private static HashedWheelTimer initHashedWheelTimer(Builder<?, ?> builder) {
        synchronized (AsyncReporter.class) {
            if (hashedWheelTimer == null) {
                hashedWheelTimer = new HashedWheelTimer(
                        hashedWheelTimerThreadFactory,
                        builder.tickDurationNanos, TimeUnit.NANOSECONDS,
                        builder.ticksPerWheel);
            }
        }
        return hashedWheelTimer;
    }

    @Override
    protected void onTimer(Message.MessageKey timerKey) {
        AbstractSizeBoundedQueue queue = queueManager.get(timerKey);
        if (queue != null && queue.size() > 0) {
            flush(queue);
        }
    }

    public io.github.suzunshou.reporter.concurrent.Future<?> flush(AbstractSizeBoundedQueue queue) {
        CompositeFuture completeFuture;
        Buffer<MessagePromise<?>> buffer = bufferPool.acquire();
        try {
            int drained = queue.drainTo(buffer);
            if (drained == 0) {
                return new SucceededFuture<>(null, null);
            }

            List<MessagePromise<R>> promises = buffer.drain();
            completeFuture = sender.send(promises);
        } finally {
            bufferPool.release(buffer);
        }

        metrics.updateQueuedMessages(queue.key, queue.size());

        if (!memoryLimiter.isMaximum()) {
            memoryLimiter.signalAll();
        }

        logFailedMessage(completeFuture);
        return completeFuture;
    }

    private void logFailedMessage(CompositeFuture completeFuture) {
        completeFuture.addListener(new GenericFutureListener<io.github.suzunshou.reporter.concurrent.Future<? super CompositeFuture>>() {
            @Override
            public void operationComplete(io.github.suzunshou.reporter.concurrent.Future<? super CompositeFuture> future) {
                if (!future.isSuccess()) {
                    MessageDroppedException droppedException = (MessageDroppedException) future.cause();
                    logger.warn(droppedException.getMessage());
                }
            }
        });
    }

    @Override
    protected ScheduledExecutorService scheduler() {
        if (this.scheduler == null) {
            synchronized (this) {
                if (this.scheduler == null) {
                    ThreadFactory threadFactory =
                            ReporterExecutors.daemonThreadFactory("AsyncReporter-" + id + "-timer-");
                    ScheduledThreadPoolExecutor timerPool = new ScheduledThreadPoolExecutor(timerThreads, threadFactory);
                    timerPool.setRemoveOnCancelPolicy(true);
                    this.scheduler = timerPool;
                    return timerPool;
                }
            }
        }
        return scheduler;
    }

    public static <M extends Message, R> Builder<M, R> builder(Sender<M, R> sender) {
        return new Builder<>(sender);
    }

    public static final class Builder<M extends Message, R> extends Reporter.Builder<M, R> {

        int nThreads = 1;
        int flushThreads = AsyncReporterProperties.DEFAULT_FLUSH_THREADS;
        int timerThreads = AsyncReporterProperties.DEFAULT_TIMER_THREADS;
        long queuedKeepAliveNanos = TimeUnit.SECONDS.toNanos(60);
        long tickDurationNanos = TimeUnit.MILLISECONDS.toNanos(100);
        int ticksPerWheel = 512;
        int totalQueuedMessages = 100000;

        Builder(Sender<M, R> sender) {
            super(sender);
        }

        @Override
        public Builder<M, R> metrics(ReporterMetrics metrics) {
            super.metrics(metrics);
            return this;
        }

        @Override
        public Builder messageTimeout(long timeout, TimeUnit unit) {
            super.messageTimeout(timeout, unit);
            return this;
        }

        @Override
        public Builder bufferedMaxMessages(int bufferedMaxMessages) {
            super.bufferedMaxMessages(bufferedMaxMessages);
            return this;
        }

        @Override
        public Builder queuedMaxMessages(int queuedMaxMessages) {
            super.queuedMaxMessages(queuedMaxMessages);
            return this;
        }


        @Override
        public Builder<M, R> overflowStrategy(OverflowStrategy.Type overflowStrategy) {
            super.overflowStrategy(overflowStrategy);
            return this;
        }

        public Builder<M, R> nThreads(int nThreads) {
            this.nThreads = nThreads;
            return this;
        }

        public Builder<M, R> flushThreads(int flushThreads) {
            this.flushThreads = flushThreads;
            return this;
        }


        public Builder<M, R> timerThreads(int timerThreads) {
            this.timerThreads = timerThreads;
            return this;
        }

        public Builder<M, R> queuedKeepAliveNanos(long queuedKeepAliveNanos, TimeUnit unit) {
            this.queuedKeepAliveNanos = unit.toNanos(queuedKeepAliveNanos);
            return this;
        }

        public Builder<M, R> tickDuration(long tickDuration, TimeUnit unit) {
            this.tickDurationNanos = unit.toNanos(tickDuration);
            return this;
        }

        public Builder<M, R> ticksPerWheel(int ticksPerWheel) {
            this.ticksPerWheel = ticksPerWheel;
            return this;
        }


        public Builder<M, R> totalQueuedMessages(int totalQueuedMessages) {
            this.totalQueuedMessages = totalQueuedMessages;
            return this;
        }

        @Override
        public Reporter<M, R> build() {
            if (totalQueuedMessages < queuedMaxMessages) {
                throw new IllegalArgumentException("totalQueuedMessages >= queuedMaxMessages");
            }
            return new AsyncReporter<>(this);
        }
    }

    @Override
    public MessageFuture<R> report(M message) {
        if (message == null) {
            throw new NullPointerException("message");
        }
        metrics.incrementMessages(1);

        if (REPORTER_STATE_UPDATER.get(this) == REPORTER_STATE_SHUTDOWN) {
            MessageDroppedException droppedException
                    = MessageDroppedException.dropped(new IllegalStateException("closed"), Collections.singletonList(message));
            MessageFuture<R> future = (MessageFuture) message.newFailedFuture(droppedException);
            setFailedListener(future);
            return future;
        }

        if (REPORTER_STATE_UPDATER.compareAndSet(this, REPORTER_STATE_INIT, REPORTER_STATE_STARTED) && messageTimeoutNanos > 0) {
            startFlushThreads();
        }

        memoryLimiter.waitWhenMaximum();

        AbstractSizeBoundedQueue queue = queueManager.getOrCreate(message.asMessageKey());
        MessagePromise<R> promise = message.newPromise();
        queue.offer(promise);
        setFailedListener(promise);

        if (queue.size() >= queuedMaxMessages) {
            flushSynchronizer.offer(queue);
        }
        return promise;
    }

    private void startFlushThreads() {
        Set<Thread> flushers = new HashSet<>(flushThreads);
        for (int i = 0; i < flushThreads; i++) {
            Thread thread = flushThreadFactory.newFlushThread();
            thread.start();
            flushers.add(thread);
        }
        this.flushers = Collections.unmodifiableSet(flushers);
    }

    private void setFailedListener(MessageFuture<R> future) {
        future.addListener(new GenericFutureListener<io.github.suzunshou.reporter.concurrent.Future<? super R>>() {
            @Override
            public void operationComplete(io.github.suzunshou.reporter.concurrent.Future<? super R> future) {
                if (!future.isSuccess()) {
                    metrics.incrementMessagesDropped(1);
                }
            }
        });
    }

    @Override
    public CheckResult check() {
        return sender.check();
    }

    @Override
    public void close() throws IOException {
        if (!REPORTER_STATE_UPDATER.compareAndSet(this, REPORTER_STATE_STARTED, REPORTER_STATE_SHUTDOWN)) {
            if (REPORTER_STATE_UPDATER.getAndSet(this, REPORTER_STATE_SHUTDOWN) != REPORTER_STATE_SHUTDOWN) {
                sender.close();
                return;
            }
        }

        flush();
        close = new CountDownLatch(messageTimeoutNanos > 0 ? flushThreads : 0);
        try {
            if (!close.await(messageTimeoutNanos * 2, TimeUnit.NANOSECONDS)) {
                logger.warn("Timed out waiting for close");
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted waiting for close");
            Thread.currentThread().interrupt();
        }

        if (scheduler != null) {
            clearTimers();
            scheduler.shutdown();
        }
        sender.close();

        int dropped = clearQueuedMessage();
        if (dropped > 0) {
            logger.warn("Dropped " + dropped + " messages due to AsyncReporter.close()");
        }
    }

    private int clearQueuedMessage() {
        int count = 0;
        for (AbstractSizeBoundedQueue queue : queueManager.elements()) {
            count += queue.size();
            metrics.removeFromQueuedMessages(queue.key);
        }
        queueManager.clear();
        if (count > 0) {
            metrics.incrementMessagesDropped(count);
        }
        return count;
    }

    @Override
    public void flush() {
        for (AbstractSizeBoundedQueue queue : queueManager.elements()) {
            Future<?> future = flush(queue);
            future.awaitUninterruptibly();
        }
    }
}
