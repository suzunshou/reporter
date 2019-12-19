package io.github.suzunshou.reporter.reporter;

/**
 * @author zunshou on 2019/11/19 9:47 上午.
 */
public interface ReporterProperties {

    <M extends Message, R> Reporter.Builder<M, R> toBuilder(Sender<M, R> sender);
}
