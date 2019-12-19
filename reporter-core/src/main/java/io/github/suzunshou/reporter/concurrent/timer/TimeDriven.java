package io.github.suzunshou.reporter.concurrent.timer;


import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zunshou on 2019/11/17 7:03 下午.
 * Schedule operations triggered by timer
 */
public abstract class TimeDriven<K> {

    static class Timer {
        final long id;
        final Future<?> task;

        Timer(long id, Future<?> task) {
            this.id = id;
            this.task = task;
        }
    }

    private final Map<K, Timer> keyToTimer = new ConcurrentHashMap<>();
    private static AtomicLong timerId = new AtomicLong();

    protected abstract void onTimer(K timerKey);

    protected abstract ScheduledExecutorService scheduler();

    public void schedulePeriodically(final K timerKey, long intervalNanos) {
        cancelTimer(timerKey);
        long id = timerId.getAndIncrement();
        ScheduledFuture<?> task = scheduler().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                onTimer(timerKey);
            }
        }, intervalNanos, intervalNanos, TimeUnit.NANOSECONDS);
        keyToTimer.put(timerKey, new Timer(id, task));
    }

    public void cancelTimer(K timerKey) {
        Timer timer = keyToTimer.get(timerKey);
        if (timer != null) {
            timer.task.cancel(false);
            keyToTimer.remove(timerKey);
        }
    }

    public boolean isTimerActive(K timerKey) {
        return keyToTimer.containsKey(timerKey);
    }

    public void clearTimers() {
        for (Timer timer : keyToTimer.values()) {
            timer.task.cancel(false);
        }
        keyToTimer.clear();
    }
}
