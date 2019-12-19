package io.github.suzunshou.reporter.reporter;

import io.github.suzunshou.reporter.concurrent.CompositeFuture;
import io.github.suzunshou.reporter.concurrent.MessagePromise;

import java.util.List;

/**
 * @author zunshou on 2019/11/16 6:25 下午.
 */
public interface AsyncSender<R> extends Component {

    /**
     * async send message
     *
     * @param promises
     * @return
     */
    CompositeFuture send(List<MessagePromise<R>> promises);
}
