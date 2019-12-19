package io.github.suzunshou.reporter.buffer;


import io.github.suzunshou.reporter.concurrent.MessagePromise;
import io.github.suzunshou.reporter.queue.MessageFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author zunshou on 2019/11/18 11:44 上午.
 */
public class Buffer implements MessageFilter {

    private int capacity;
    private final List<Object> elements = new ArrayList<>();
    private boolean bufferFull;

    Buffer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean accept(MessagePromise<?> promise) {
        if (bufferFull) {
            return false;
        }
        elements.add(promise);
        if (elements.size() == capacity) {
            bufferFull = true;
        }
        return true;
    }

    public <T> T drain() {
        if (elements.isEmpty()) {
            return (T) Collections.emptyList();
        }
        Object result = new ArrayList<>(elements);
        clear();
        return (T) result;
    }

    public void clear() {
        elements.clear();
        bufferFull = false;
    }
}
