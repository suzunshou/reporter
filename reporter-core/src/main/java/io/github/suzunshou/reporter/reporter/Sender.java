package io.github.suzunshou.reporter.reporter;

import java.util.List;

/**
 * @author zunshou on 2019/11/16 4:32 下午.
 */
public interface Sender<M extends Message, R> extends Component {

    /**
     * batch send messages
     *
     * @param messages
     * @return
     */
    List<R> send(List<M> messages);
}
