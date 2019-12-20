package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.buffers.BufferFilter;
import io.github.suzunshou.reporter.concurrent.MessagePromise;
import io.github.suzunshou.reporter.concurrent.Promises;
import io.github.suzunshou.reporter.reporter.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author zunshou on 2019/11/17 6:00 下午.
 */
final class ConcurrentSizeBoundedQueue extends AbstractSizeBoundedQueue {

    private final AtomicInteger size = new AtomicInteger();

    private final OverflowStrategy.Type overflowStrategy;

    private final ConcurrentLinkedDeque<MessagePromise<?>> deque = new ConcurrentLinkedDeque<>();

    private final ReentrantLock lock;
    private final Condition notFull;

    public ConcurrentSizeBoundedQueue(int maxSize, Message.MessageKey key, OverflowStrategy.Type overflowStrategy) {
        super(maxSize, key);
        this.overflowStrategy = overflowStrategy;
        if (overflowStrategy == OverflowStrategy.Type.BackPressure) {
            lock = new ReentrantLock();
            notFull = lock.newCondition();
        } else {
            lock = null;
            notFull = null;
        }
    }


    @Override
    public void offer(MessagePromise<?> promise) {
        for (; ; ) {
            int currSize = size.get();
            if (currSize >= maxSize) {
                switch (overflowStrategy) {
                    case DropNew:
                        if ((currSize = size.get()) >= maxSize) {
                            promise.setFailure(MessageDroppedException.dropped(OverflowStrategy.Type.DropNew.getStrategy(), promise.message()));
                        }
                        break;
                    case DropTail:
                        if (size.compareAndSet(currSize, currSize - 1)) {
                            MessagePromise<?> tail = deque.pollLast();
                            if (tail != null) {
                                tail.setFailure(MessageDroppedException.dropped(OverflowStrategy.Type.DropTail.getStrategy(), tail.message()));
                            } else {
                                size.incrementAndGet();
                            }
                        }
                        continue;
                    case DropHead:
                        if (size.compareAndSet(currSize, currSize - 1)) {
                            MessagePromise<?> head = deque.pollFirst();
                            if (head != null) {
                                head.setFailure(MessageDroppedException.dropped(OverflowStrategy.Type.DropHead.getStrategy(), head.message()));
                            } else {
                                size.incrementAndGet();
                            }
                        }
                        continue;
                    case DropBuffer:
                        List<MessagePromise<?>> promises = dropBuffer(deque);
                        int dropped;
                        if ((dropped = promises.size()) > 0) {
                            size.addAndGet(-dropped);
                        }
                        Promises.allFail(promises, OverflowStrategy.Type.DropBuffer.getStrategy());
                        continue;
                    case BackPressure:
                        doBackPressure();
                        continue;
                    case Fail:
                        if ((currSize = size.get()) >= maxSize) {
                            throw new BufferOverflowException("Max size of " + maxSize + " is reached. currSize is " + currSize);
                        }
                        break;
                }
            }

            if (size.compareAndSet(currSize, currSize + 1)) {
                deque.offer(promise);
                return;
            }
        }
    }

    public void doBackPressure() {
        boolean interrupted = false;
        lock.lock();
        try {
            while (size.get() >= maxSize) {
                try {
                    notFull.await();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            lock.unlock();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<MessagePromise<?>> dropBuffer(ConcurrentLinkedDeque<MessagePromise<?>> deque) {
        List<MessagePromise<?>> result = new LinkedList<>();
        MessagePromise<?> promise;
        while ((promise = deque.pollFirst()) != null) {
            result.add(promise);
        }
        return result;
    }

    @Override
    public int drainTo(BufferFilter<MessagePromise<?>> filter) {
        MessagePromise<?> promise;
        int drained = 0;

        while ((promise = deque.poll()) != null) {
            if (!filter.accept(promise)) {
                deque.offerFirst(promise);
                break;
            }
            drained++;
        }

        size.addAndGet(-drained);

        if (notFull != null && drained > 0) {
            lock.lock();
            try {
                for (int count = drained; count > 0 && lock.hasWaiters(notFull); count--) {
                    notFull.signal();
                }
            } finally {
                lock.unlock();
            }
        }

        return drained;
    }

    @Override
    public int clear() {
        return drainTo(new BufferFilter<MessagePromise<?>>() {
            @Override
            public boolean accept(MessagePromise<?> promise) {
                return true;
            }
        });
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
