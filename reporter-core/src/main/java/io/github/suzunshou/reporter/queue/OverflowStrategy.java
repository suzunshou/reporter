package io.github.suzunshou.reporter.queue;

/**
 * @author zunshou on 2019/11/17 12:56 下午.
 * Represents a strategy that decides how to deal with a buffer of time based operator
 * that is full but is about to receive a new element.
 */
public abstract class OverflowStrategy {

    public enum Type {
        DropHead(1, dropHead),
        DropTail(2, dropTail),
        DropBuffer(3, dropBuffer),
        DropNew(4, dropNew),
        BackPressure(5, backPressure),
        Fail(6, fail);
        private int value;
        private OverflowStrategy strategy;

        Type(int value, OverflowStrategy strategy) {
            this.value = value;
            this.strategy = strategy;
        }

        public int getValue() {
            return value;
        }

        public OverflowStrategy getStrategy() {
            return strategy;
        }

        public static OverflowStrategy get(int value) {
            switch (value) {
                case 1:
                    return DropHead.getStrategy();
                case 2:
                    return DropTail.getStrategy();
                case 3:
                    return DropBuffer.getStrategy();
                case 4:
                    return DropNew.getStrategy();
                case 5:
                    return BackPressure.getStrategy();
                case 6:
                    return Fail.getStrategy();
                default:
                    return null;
            }
        }
    }


    /**
     * If the buffer is full when a new element arrives, drops the oldest element from the buffer to make space for
     * the new element.
     */
    private static final OverflowStrategy dropHead = new DropHead();

    /**
     * If the buffer is full when a new element arrives, drops the youngest element from the buffer to make space for
     * the new element.
     */
    private static final OverflowStrategy dropTail = new DropTail();

    /**
     * If the buffer is full when a new element arrives, drops all the buffered elements to make space for the new element.
     */
    private static final OverflowStrategy dropBuffer = new DropBuffer();

    /**
     * If the buffer is full when a new element arrives, drops the new element.
     */
    private static final OverflowStrategy dropNew = new DropNew();

    /**
     * If the buffer is full when a new element is available this strategy backpressures the upstream publisher until
     * space becomes available in the buffer.
     */
    private static final OverflowStrategy backPressure = new BackPressure();

    /**
     * If the buffer is full when a new element is available this strategy completes the stream with failure.
     */
    private static final OverflowStrategy fail = new Fail();

    abstract boolean isBackPressure();

    private static class DropHead extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return false;
        }

        @Override
        public String toString() {
            return "DropHead";
        }
    }

    private static class DropTail extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return false;
        }

        @Override
        public String toString() {
            return "DropTail";
        }
    }

    private static class DropBuffer extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return false;
        }

        @Override
        public String toString() {
            return "DropBuffer";
        }
    }

    private static class DropNew extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return false;
        }

        @Override
        public String toString() {
            return "DropNew";
        }
    }

    private static class BackPressure extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return true;
        }

        @Override
        public String toString() {
            return "BackPressure";
        }
    }

    private static class Fail extends OverflowStrategy {
        @Override
        boolean isBackPressure() {
            return false;
        }

        @Override
        public String toString() {
            return "Fail";
        }
    }
}
