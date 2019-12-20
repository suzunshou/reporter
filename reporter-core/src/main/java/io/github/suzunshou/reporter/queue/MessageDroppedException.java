package io.github.suzunshou.reporter.queue;

import io.github.suzunshou.reporter.reporter.Message;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @author zunshou on 2019/11/17 3:05 下午.
 */
public class MessageDroppedException extends RuntimeException {

    private static final long serialVersionUID = 7409445760101741553L;

    final List<? extends Message> droppedMessages;
    final boolean overflow;

    public MessageDroppedException(OverflowStrategy overflowStrategy, List<Message> droppedMessages) {
        super("Dropping messages count is " + droppedMessages.size() + " and overflowStrategy is " + overflowStrategy);
        this.droppedMessages = droppedMessages;
        this.overflow = true;
    }

    private MessageDroppedException(Throwable cause, List<? extends Message> droppedMessages) {
        super(String.format("Dropping %d messages for %s(%s)", droppedMessages.size(), cause.getClass().getSimpleName(),
                cause.getMessage() == null ? "" : cause.getMessage()), cause);
        this.droppedMessages = droppedMessages;
        this.overflow = false;
    }

    public static MessageDroppedException dropped(OverflowStrategy overflowStrategy, Message dropped) {
        return new MessageDroppedException(overflowStrategy, singletonList(dropped));
    }

    public static MessageDroppedException dropped(OverflowStrategy overflowStrategy, List<Message> dropped) {
        return new MessageDroppedException(overflowStrategy, dropped);
    }

    public static MessageDroppedException dropped(Throwable cause, List<? extends Message> dropped) {
        return new MessageDroppedException(cause, dropped);
    }
}
