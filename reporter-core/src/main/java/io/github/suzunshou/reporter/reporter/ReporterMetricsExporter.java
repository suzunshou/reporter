package io.github.suzunshou.reporter.reporter;

import java.io.Closeable;

/**
 * @author zunshou on 2019/11/19 10:06 上午.
 */
public abstract class ReporterMetricsExporter implements Closeable {

    public abstract void start(ReporterMetrics metrics);

    public static ReporterMetricsExporter of(String type) {
        if ("log".equals(type.toLowerCase())) {
            return new LogReporterMetricsExporter();
        }
        return NOOP_EXPORTER;
    }

    @Override
    public abstract void close();

    static final ReporterMetricsExporter NOOP_EXPORTER = new ReporterMetricsExporter() {
        @Override
        public void start(ReporterMetrics metrics) {
        }

        public void close() {
        }
    };
}
